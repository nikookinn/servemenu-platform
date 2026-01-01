/**
 * QR Generator with node-canvas - BEST PERFORMANCE + BEST QUALITY!
 * 10-20x faster than Puppeteer, same visual quality
 * Uses qr-code-styling with node-canvas (native C++ binding)
 */

import { createCanvas, loadImage } from 'canvas';
import { JSDOM } from 'jsdom';
import type { QRCustomization } from '../types/qr-customization.types';
import { logger } from '../utils/logger';
import { mediaClientService } from './media-client.service';

// @ts-ignore - qr-code-styling has no types
const { QRCodeStyling } = require('qr-code-styling/lib/qr-code-styling.common.js');

export class QRGeneratorCanvasService {
  /**
   * Generate QR code with full customization using node-canvas
   * ~100-200ms vs 1,300ms with Puppeteer!
   */
  async generateQR(url: string, customization: QRCustomization): Promise<string> {
    const startTime = Date.now();

    try {
      logger.info('🎨 Generating QR with node-canvas (fast + beautiful!)', {
        url,
        customization: {
          id: customization.id,
          frameType: customization.frameType,
          patternType: customization.patternType,
          eyeType: customization.eyeType,
          backgroundColor: customization.backgroundColor,
          patternColorSingle: customization.patternColorSingle,
          frameText: customization.frameText,
          hasLogo: !!customization.logoMediaId
        }
      });

      // Build QR options (async because it may fetch logo)
      const options = await this.buildQROptions(url, customization);

      // Create QR with node-canvas
      const qrCode = new (QRCodeStyling as any)({
        jsdom: JSDOM,
        nodeCanvas: { createCanvas, loadImage },
        ...options,
        imageOptions: {
          ...options.imageOptions,
          saveAsBlob: true,
          crossOrigin: 'anonymous',
          margin: 20
        }
      });

      // Get PNG buffer
      let buffer = await qrCode.getRawData('png');
      
      // Add frame if needed, otherwise center in 800x800
      if (customization.frameType && customization.frameType !== 'none') {
        buffer = await this.addFrame(buffer, customization);
      } else {
        buffer = await this.centerIn800x800(buffer);
      }
      
      // Convert to base64
      const base64 = `data:image/png;base64,${buffer.toString('base64')}`;

      const duration = Date.now() - startTime;
      logger.info(`✅ Canvas QR generated in ${duration}ms`, {
        url,
        size: `${(buffer.length / 1024).toFixed(2)} KB`,
        hasFrame: customization.frameType !== 'none'
      });

      return base64;

    } catch (error) {
      logger.error('❌ Canvas QR generation failed', {
        error: error instanceof Error ? {
          message: error.message,
          stack: error.stack
        } : error
      });
      throw error;
    }
  }

  /**
   * Build QR options from customization
   * Async because it fetches logo from Media Service if needed
   */
  private async buildQROptions(url: string, customization: QRCustomization): Promise<any> {
    const options: any = {
      width: 600,
      height: 600,
      data: url,
      margin: customization.margin || 1,
      qrOptions: {
        errorCorrectionLevel: 'H' // High error correction for logos
      },
      backgroundOptions: {
        color: customization.backgroundColor || '#FFFFFF'
      }
    };

    // Pattern (dots) customization
    options.dotsOptions = {
      type: this.mapPatternType(customization.patternType),
      color: customization.patternColorSingle || '#000000'
    };

    // Gradient support
    if (customization.patternColorMode === 'gradient') {
      options.dotsOptions.gradient = {
        type: customization.patternGradientType || 'linear',
        rotation: customization.patternGradientRotation || 0,
        colorStops: [
          { offset: 0, color: customization.patternGradientStart || '#000000' },
          { offset: 1, color: customization.patternGradientEnd || '#000000' }
        ]
      };
    }

    // Eye (corner squares) customization
    options.cornersSquareOptions = {
      type: this.mapEyeType(customization.eyeType),
      color: customization.eyeColorEnabled 
        ? customization.eyeColorOuter 
        : customization.patternColorSingle
    };

    options.cornersDotOptions = {
      type: this.mapEyeType(customization.eyeType),
      color: customization.eyeColorEnabled 
        ? customization.eyeColorInner 
        : customization.patternColorSingle
    };

    // Logo support - Fetch from Media Service
    if (customization.logoMediaId) {
      try {
        logger.info('🖼️ Fetching logo for QR code', { logoMediaId: customization.logoMediaId });
        
        // Fetch logo URL from Media Service
        const logoUrl = await mediaClientService.fetchLogoUrl(customization.logoMediaId);
        
        if (logoUrl) {
          // Download logo image
          const logoBuffer = await mediaClientService.downloadLogoImage(logoUrl);
          
          if (logoBuffer) {
            // Convert buffer to data URL for qr-code-styling
            const logoDataUrl = `data:image/png;base64,${logoBuffer.toString('base64')}`;
            
            options.image = logoDataUrl;
            options.imageOptions = {
              hideBackgroundDots: true,
              imageSize: (customization.logoSize || 30) / 100, // Convert percentage to decimal (30% → 0.3)
              margin: 5, // Reduced margin for better logo visibility
              crossOrigin: 'anonymous',
              saveAsBlob: true
            };
            
            logger.info('✅ Logo embedded in QR code', { 
              logoSize: `${customization.logoSize || 30}%`,
              logoSizeBytes: logoBuffer.length
            });
          } else {
            logger.warn('⚠️ Logo download failed, generating QR without logo');
          }
        } else {
          logger.warn('⚠️ Logo URL not found, generating QR without logo');
        }
      } catch (error) {
        logger.error('❌ Error fetching logo, generating QR without logo', {
          error: error instanceof Error ? error.message : error
        });
        // Graceful degradation - continue without logo
      }
    }

    return options;
  }

  /**
   * Map pattern type to qr-code-styling format
   */
  private mapPatternType(type?: string): string {
    const mapping: Record<string, string> = {
      'square': 'square',
      'rounded': 'rounded',
      'dots': 'dots',
      'classy': 'classy',
      'classy-rounded': 'classy-rounded'
    };
    return mapping[type || 'rounded'] || 'rounded';
  }

  /**
   * Map eye type to qr-code-styling format
   */
  private mapEyeType(type?: string): string {
    const mapping: Record<string, string> = {
      'square': 'square',
      'dot': 'dot', // Fully rounded eyes
      'rounded': 'dot', // Alias for dot
      'extra-rounded': 'extra-rounded',
      'classy': 'classy',
      'classy-rounded': 'classy-rounded'
    };
    return mapping[type || 'square'] || 'square';
  }

  /**
   * Add frame to QR code (border + text area)
   * Final size: 800x800 with transparent sides
   * Supports: bottom-text, top-text, circle-frame, box-frame
   */
  private async addFrame(qrBuffer: Buffer, customization: QRCustomization): Promise<Buffer> {
    try {
      const frameType = customization.frameType;
      
      logger.info('🎨 Adding frame', { 
        frameType, 
        hasText: !!customization.frameText 
      });

      // Route to specific frame type handler
      switch (frameType) {
        case 'bottom-text':
          return await this.addBottomTextFrame(qrBuffer, customization);
        case 'top-text':
          return await this.addTopTextFrame(qrBuffer, customization);
        case 'circle-frame':
          return await this.addCircleFrame(qrBuffer, customization);
        case 'box-frame':
          return await this.addBoxFrame(qrBuffer, customization);
        default:
          logger.warn('⚠️ Unknown frame type, using bottom-text', { frameType });
          return await this.addBottomTextFrame(qrBuffer, customization);
      }
      
    } catch (error) {
      logger.error('❌ Failed to add frame', { error });
      return qrBuffer; // Return original if frame fails
    }
  }

  /**
   * Bottom text frame: QR at top, text area at bottom
   */
  private async addBottomTextFrame(qrBuffer: Buffer, customization: QRCustomization): Promise<Buffer> {
    const qrImage = await loadImage(qrBuffer);
    
    const canvasWidth = 800;
    const canvasHeight = 800;
    const frameWidth = 640;
    const frameHeight = 800;
    const borderWidth = 20;
    const textAreaHeight = 140;
    
    const availableQRHeight = frameHeight - textAreaHeight - (borderWidth * 2);
    const availableQRWidth = frameWidth - (borderWidth * 2);
    const qrSize = Math.min(availableQRWidth, availableQRHeight);
    
    const offsetX = (canvasWidth - frameWidth) / 2;
    const offsetY = 0;
    
    const canvas = createCanvas(canvasWidth, canvasHeight);
    const ctx = canvas.getContext('2d');
    ctx.clearRect(0, 0, canvasWidth, canvasHeight);
    
    // Frame background
    const frameColor = customization.frameColorSingle || '#000000';
    ctx.fillStyle = frameColor;
    ctx.fillRect(offsetX, offsetY, frameWidth, frameHeight);
    
    // QR white background
    const qrOffsetX = offsetX + borderWidth;
    const qrOffsetY = offsetY + borderWidth;
    ctx.fillStyle = '#FFFFFF';
    ctx.fillRect(qrOffsetX, qrOffsetY, qrSize, qrSize);
    
    // Draw QR
    ctx.drawImage(qrImage, qrOffsetX, qrOffsetY, qrSize, qrSize);
    
    // Draw text at bottom
    if (customization.frameText) {
      const textColor = customization.frameTextColor || '#FFFFFF';
      ctx.fillStyle = textColor;
      const fontSize = 32; // Fixed font size
      const fontFamily = customization.frameFont || 'Arial';
      ctx.font = `bold ${fontSize}px ${fontFamily}, sans-serif`;
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';
      
      const qrBottomY = qrOffsetY + qrSize;
      const textAreaStartY = qrBottomY;
      const textY = textAreaStartY + (textAreaHeight / 2);
      
      const maxWidth = frameWidth - 60;
      const lines = this.wrapText(ctx, customization.frameText, maxWidth);
      const lineHeight = fontSize * 1.2;
      const totalTextHeight = lines.length * lineHeight;
      const startY = textY - (totalTextHeight / 2) + (lineHeight / 2);
      
      lines.forEach((line: string, index: number) => {
        const lineY = startY + (index * lineHeight);
        ctx.fillText(line, canvasWidth / 2, lineY);
      });
      
      logger.info('✅ Bottom text drawn', { 
        lines: lines.length, 
        textY, 
        qrBottomY,
        textAreaStartY,
        text: customization.frameText,
        color: textColor 
      });
    }
    
    return canvas.toBuffer('image/png');
  }

  /**
   * Top text frame: Text area at top, QR at bottom
   */
  private async addTopTextFrame(qrBuffer: Buffer, customization: QRCustomization): Promise<Buffer> {
    const qrImage = await loadImage(qrBuffer);
    
    const canvasWidth = 800;
    const canvasHeight = 800;
    const frameWidth = 640;
    const frameHeight = 800;
    const borderWidth = 20;
    const textAreaHeight = 140;
    
    // Same calculation as bottom frame
    const availableQRHeight = frameHeight - textAreaHeight - (borderWidth * 2);
    const availableQRWidth = frameWidth - (borderWidth * 2);
    const qrSize = Math.min(availableQRWidth, availableQRHeight); // 600px like bottom frame
    
    const offsetX = (canvasWidth - frameWidth) / 2;
    const offsetY = 0;
    
    const canvas = createCanvas(canvasWidth, canvasHeight);
    const ctx = canvas.getContext('2d');
    ctx.clearRect(0, 0, canvasWidth, canvasHeight);
    
    // Frame background
    const frameColor = customization.frameColorSingle || '#000000';
    ctx.fillStyle = frameColor;
    ctx.fillRect(offsetX, offsetY, frameWidth, frameHeight);
    
    // QR white background (positioned AFTER text area, with extra offset to balance bottom border)
    const qrOffsetX = offsetX + borderWidth;
    // Add extra 20px to move QR down and make borders more equal
    const qrOffsetY = offsetY + textAreaHeight + borderWidth + 20;
    ctx.fillStyle = '#FFFFFF';
    ctx.fillRect(qrOffsetX, qrOffsetY, qrSize, qrSize);
    
    // Draw QR
    ctx.drawImage(qrImage, qrOffsetX, qrOffsetY, qrSize, qrSize);
    
    // Draw text at TOP
    if (customization.frameText) {
      const textColor = customization.frameTextColor || '#FFFFFF';
      ctx.fillStyle = textColor;
      const fontSize = 32; // Fixed font size
      const fontFamily = customization.frameFont || 'Arial';
      ctx.font = `bold ${fontSize}px ${fontFamily}, sans-serif`;
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';
      
      const textAreaStartY = offsetY;
      const textY = textAreaStartY + (textAreaHeight / 2);
      
      const maxWidth = frameWidth - 60;
      const lines = this.wrapText(ctx, customization.frameText, maxWidth);
      const lineHeight = fontSize * 1.2;
      const totalTextHeight = lines.length * lineHeight;
      const startY = textY - (totalTextHeight / 2) + (lineHeight / 2);
      
      lines.forEach((line: string, index: number) => {
        const lineY = startY + (index * lineHeight);
        ctx.fillText(line, canvasWidth / 2, lineY);
      });
      
      logger.info('✅ Top text drawn', { 
        lines: lines.length, 
        textY,
        textAreaStartY,
        text: customization.frameText,
        color: textColor 
      });
    }
    
    return canvas.toBuffer('image/png');
  }

  /**
   * Circle frame: QR perfectly fitted in circle, text on frame at bottom
   * Design: Full 800x800 circle with QR inside (no corners visible), text overlaid on bottom
   */
  private async addCircleFrame(qrBuffer: Buffer, customization: QRCustomization): Promise<Buffer> {
    const qrImage = await loadImage(qrBuffer);
    
    const canvasSize = 800;
    const circleRadius = 380; // Larger circle to fill canvas better
    const borderWidth = 20;
    
    const canvas = createCanvas(canvasSize, canvasSize);
    const ctx = canvas.getContext('2d');
    ctx.clearRect(0, 0, canvasSize, canvasSize);
    
    const centerX = canvasSize / 2;
    const centerY = canvasSize / 2; // Perfect center
    
    const frameColor = customization.frameColorSingle || '#000000';
    
    // 1. Draw outer circle (border)
    ctx.fillStyle = frameColor;
    ctx.beginPath();
    ctx.arc(centerX, centerY, circleRadius + borderWidth, 0, Math.PI * 2);
    ctx.fill();
    
    // 2. Draw inner circle (FRAME COLOR - not white!)
    ctx.fillStyle = frameColor;
    ctx.beginPath();
    ctx.arc(centerX, centerY, circleRadius, 0, Math.PI * 2);
    ctx.fill();
    
    // 3. Clip to circle and draw QR code
    ctx.save();
    ctx.beginPath();
    ctx.arc(centerX, centerY, circleRadius, 0, Math.PI * 2);
    ctx.clip();
    
    // QR smaller than inscribed square (85% of inscribed square)
    // For a square to fit in a circle: side = diameter / sqrt(2)
    const inscribedSquare = (circleRadius * 2) / Math.sqrt(2);
    const qrSize = inscribedSquare * 0.85; // 85% of inscribed square
    const qrX = centerX - qrSize / 2;
    const qrY = centerY - qrSize / 2;
    ctx.drawImage(qrImage, qrX, qrY, qrSize, qrSize);
    ctx.restore();
    
    // 4. Draw text ON the circle frame at bottom (overlaid)
    if (customization.frameText) {
      const textColor = customization.frameTextColor || '#FFFFFF'; // White text on dark frame
      ctx.fillStyle = textColor;
      
      // Dynamic font size based on text length
      const textLength = customization.frameText.length;
      let fontSize: number;
      if (textLength > 50) {
        fontSize = 18; // Small for very long text
      } else if (textLength > 30) {
        fontSize = 20; // Medium for long text
      } else {
        fontSize = 24; // Normal for short text
      }
      
      const fontFamily = customization.frameFont || 'Arial';
      ctx.font = `bold ${fontSize}px ${fontFamily}, sans-serif`;
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';
      
      // Text position: on the bottom part of the circle (higher up)
      const textY = centerY + circleRadius - 90; // 90px from bottom of circle (adjusted for larger circle)
      
      // Max width for text (fit within circle width, accounting for curve)
      const maxWidth = circleRadius * 2 - 180; // More margin for safety (adjusted for larger circle)
      const lines = this.wrapText(ctx, customization.frameText, maxWidth);
      const lineHeight = fontSize * 1.15; // Tighter line spacing
      const totalTextHeight = lines.length * lineHeight;
      const startY = textY - (totalTextHeight / 2) + (lineHeight / 2);
      
      // Draw each line
      lines.forEach((line: string, index: number) => {
        const lineY = startY + (index * lineHeight);
        ctx.fillText(line, centerX, lineY);
      });
      
      logger.info('✅ Circle frame drawn', { 
        lines: lines.length,
        circleRadius,
        qrSize: Math.round(qrSize),
        textY,
        fontSize,
        textLength,
        text: customization.frameText,
        color: textColor 
      });
    }
    
    return canvas.toBuffer('image/png');
  }

  /**
   * Box frame: Simple QR with border frame only (no text)
   * Design: Full 800x800 square frame with QR inside
   */
  private async addBoxFrame(qrBuffer: Buffer, customization: QRCustomization): Promise<Buffer> {
    const qrImage = await loadImage(qrBuffer);
    
    const canvasSize = 800;
    const frameSize = 800; // Full canvas size
    const borderWidth = 20;
    
    const canvas = createCanvas(canvasSize, canvasSize);
    const ctx = canvas.getContext('2d');
    ctx.clearRect(0, 0, canvasSize, canvasSize);
    
    const frameColor = customization.frameColorSingle || '#000000';
    
    // Draw border frame (full canvas)
    ctx.fillStyle = frameColor;
    ctx.fillRect(0, 0, frameSize, frameSize);
    
    // Draw white background for QR
    const qrInnerX = borderWidth;
    const qrInnerY = borderWidth;
    const qrInnerSize = frameSize - (borderWidth * 2);
    
    ctx.fillStyle = '#FFFFFF';
    ctx.fillRect(qrInnerX, qrInnerY, qrInnerSize, qrInnerSize);
    
    // Draw QR code (centered in white area, larger - 95% instead of 90%)
    const qrSize = qrInnerSize * 0.95; // Increased from 0.9 to 0.95
    const qrX = qrInnerX + (qrInnerSize - qrSize) / 2;
    const qrY = qrInnerY + (qrInnerSize - qrSize) / 2;
    ctx.drawImage(qrImage, qrX, qrY, qrSize, qrSize);
    
    logger.info('✅ Box frame drawn (full 800x800, no text)', { 
      frameSize,
      borderWidth,
      qrSize,
      frameColor
    });
    
    return canvas.toBuffer('image/png');
  }

  /**
   * Scale frameless QR to fill 800x800 canvas (no centering, full size)
   */
  private async centerIn800x800(qrBuffer: Buffer): Promise<Buffer> {
    try {
      // Load QR image
      const qrImage = await loadImage(qrBuffer);
      
      const finalSize = 800;
      
      // Create 800x800 canvas
      const canvas = createCanvas(finalSize, finalSize);
      const ctx = canvas.getContext('2d');
      
      // Clear canvas (transparent background)
      ctx.clearRect(0, 0, finalSize, finalSize);
      
      // Draw QR code scaled to full 800x800 (no centering offset)
      ctx.drawImage(qrImage, 0, 0, finalSize, finalSize);
      
      logger.info('✅ Frameless QR scaled to 800x800');
      
      // Convert to buffer
      return canvas.toBuffer('image/png');
      
    } catch (error) {
      logger.error('❌ Failed to scale QR', { error });
      return qrBuffer; // Return original if scaling fails
    }
  }

  /**
   * Wrap text to fit within max width, breaking at word boundaries
   */
  private wrapText(ctx: any, text: string, maxWidth: number): string[] {
    const words = text.split(' ');
    const lines: string[] = [];
    let currentLine = '';

    for (const word of words) {
      const testLine = currentLine ? `${currentLine} ${word}` : word;
      const metrics = ctx.measureText(testLine);
      
      if (metrics.width > maxWidth && currentLine) {
        lines.push(currentLine);
        currentLine = word;
      } else {
        currentLine = testLine;
      }
    }
    
    if (currentLine) {
      lines.push(currentLine);
    }
    
    // Limit to 2 lines max for frame
    return lines.slice(0, 2);
  }

}
