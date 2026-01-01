/**
 * Test script for box frame design
 * Run: node test-box-frame.js
 */

const { createCanvas, loadImage } = require('canvas');
const fs = require('fs');

async function testBoxFrame() {
  console.log('🧪 Testing box frame with decorative notch...\n');

  const canvasSize = 800;
  const frameWidth = 640;
  const qrFrameHeight = 580;
  const textFrameHeight = 140;
  const gap = 0;
  const borderWidth = 20;
  const notchWidth = 120;
  const notchHeight = 30;
  
  const canvas = createCanvas(canvasSize, canvasSize);
  const ctx = canvas.getContext('2d');
  ctx.clearRect(0, 0, canvasSize, canvasSize);
  
  const offsetX = (canvasSize - frameWidth) / 2;
  const frameColor = '#000000';
  
  // 1. Draw QR Frame (top box)
  const qrFrameY = (canvasSize - qrFrameHeight - textFrameHeight - gap) / 2;
  ctx.fillStyle = frameColor;
  ctx.fillRect(offsetX, qrFrameY, frameWidth, qrFrameHeight);
  
  console.log(`QR Frame: Y=${qrFrameY}, Height=${qrFrameHeight}`);
  
  // QR white background
  const qrInnerX = offsetX + borderWidth;
  const qrInnerY = qrFrameY + borderWidth;
  const qrInnerWidth = frameWidth - (borderWidth * 2);
  const qrInnerHeight = qrFrameHeight - (borderWidth * 2);
  
  ctx.fillStyle = '#FFFFFF';
  ctx.fillRect(qrInnerX, qrInnerY, qrInnerWidth, qrInnerHeight);
  
  // Draw fake QR (red square)
  const qrSize = Math.min(qrInnerWidth, qrInnerHeight) * 0.9;
  const qrX = qrInnerX + (qrInnerWidth - qrSize) / 2;
  const qrY = qrInnerY + (qrInnerHeight - qrSize) / 2;
  
  ctx.fillStyle = '#FF0000';
  ctx.fillRect(qrX, qrY, qrSize, qrSize);
  
  console.log(`QR Code: ${qrX}, ${qrY}, ${qrSize}x${qrSize}`);
  
  // 2. Draw Text Frame (bottom box with decorative notch)
  const textFrameY = qrFrameY + qrFrameHeight + gap;
  
  console.log(`Text Frame: Y=${textFrameY}, Height=${textFrameHeight}`);
  console.log(`Notch: Width=${notchWidth}, Height=${notchHeight}`);
  
  // Draw text frame with notch
  ctx.fillStyle = frameColor;
  ctx.beginPath();
  
  // Start from top-left
  ctx.moveTo(offsetX, textFrameY);
  
  // Top edge with decorative notch in the middle
  const notchStartX = offsetX + (frameWidth - notchWidth) / 2;
  ctx.lineTo(notchStartX, textFrameY);
  
  // Decorative notch (upward curve)
  ctx.lineTo(notchStartX, textFrameY - notchHeight);
  ctx.arcTo(
    notchStartX + notchWidth / 2, textFrameY - notchHeight - 10,
    notchStartX + notchWidth, textFrameY - notchHeight,
    20
  );
  ctx.lineTo(notchStartX + notchWidth, textFrameY);
  
  // Continue edges
  ctx.lineTo(offsetX + frameWidth, textFrameY);
  ctx.lineTo(offsetX + frameWidth, textFrameY + textFrameHeight);
  ctx.lineTo(offsetX, textFrameY + textFrameHeight);
  ctx.lineTo(offsetX, textFrameY);
  
  ctx.closePath();
  ctx.fill();
  
  // 3. Draw text
  const testText = 'SCAN FOR MENU';
  ctx.fillStyle = '#FFFFFF';
  const fontSize = 32;
  ctx.font = `bold ${fontSize}px Arial, sans-serif`;
  ctx.textAlign = 'center';
  ctx.textBaseline = 'middle';
  
  const textY = textFrameY + (textFrameHeight / 2);
  ctx.fillText(testText, canvasSize / 2, textY);
  
  console.log(`Text: Y=${textY}`);
  
  // Draw debug lines
  // Green line at text Y
  ctx.strokeStyle = '#00FF00';
  ctx.lineWidth = 2;
  ctx.beginPath();
  ctx.moveTo(0, textY);
  ctx.lineTo(canvasSize, textY);
  ctx.stroke();
  
  // Blue line at notch top
  ctx.strokeStyle = '#0000FF';
  ctx.beginPath();
  ctx.moveTo(0, textFrameY - notchHeight);
  ctx.lineTo(canvasSize, textFrameY - notchHeight);
  ctx.stroke();
  
  console.log(`\n✅ Design:`);
  console.log(`  - QR Frame: ${qrFrameHeight}px (black border, white inside)`);
  console.log(`  - Text Frame: ${textFrameHeight}px (black, below QR)`);
  console.log(`  - Decorative Notch: ${notchWidth}x${notchHeight}px (upward curve)`);
  console.log(`  - Gap between frames: ${gap}px`);
  
  // Save to file
  const buffer = canvas.toBuffer('image/png');
  fs.writeFileSync('test-box-frame.png', buffer);
  console.log(`\n💾 Saved to test-box-frame.png`);
  console.log(`\nGreen line = Text position`);
  console.log(`Blue line = Notch top`);
}

testBoxFrame().catch(console.error);
