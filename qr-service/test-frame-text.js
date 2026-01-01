/**
 * Test script to verify frame text rendering
 * Run: node test-frame-text.js
 */

const { createCanvas, loadImage } = require('canvas');
const fs = require('fs');

async function testFrameText() {
  console.log('🧪 Testing frame text rendering...\n');

  // Test configuration
  const frameTypes = ['bottom-text', 'top-text'];
  const testText = 'SCAN FOR MENU';
  
  for (const frameType of frameTypes) {
    console.log(`\n📝 Testing ${frameType}...`);
    
    const canvasWidth = 800;
    const canvasHeight = 800;
    const frameWidth = 640;
    const frameHeight = 800;
    const borderWidth = 20;
    const textAreaHeight = 140;
    
    const offsetX = (canvasWidth - frameWidth) / 2;
    const offsetY = 0;
    
    const canvas = createCanvas(canvasWidth, canvasHeight);
    const ctx = canvas.getContext('2d');
    ctx.clearRect(0, 0, canvasWidth, canvasHeight);
    
    // Frame background (black)
    ctx.fillStyle = '#000000';
    ctx.fillRect(offsetX, offsetY, frameWidth, frameHeight);
    
    // Calculate positions
    let qrOffsetX, qrOffsetY, textY;
    
    if (frameType === 'bottom-text') {
      // QR at top
      qrOffsetX = offsetX + borderWidth;
      qrOffsetY = offsetY + borderWidth;
      
      const availableQRHeight = frameHeight - textAreaHeight - (borderWidth * 2);
      const availableQRWidth = frameWidth - (borderWidth * 2);
      const qrSize = Math.min(availableQRWidth, availableQRHeight);
      
      // White background for QR
      ctx.fillStyle = '#FFFFFF';
      ctx.fillRect(qrOffsetX, qrOffsetY, qrSize, qrSize);
      
      // Draw fake QR (red square for visibility)
      ctx.fillStyle = '#FF0000';
      ctx.fillRect(qrOffsetX + 50, qrOffsetY + 50, qrSize - 100, qrSize - 100);
      
      // Text position
      const qrBottomY = qrOffsetY + qrSize;
      const textAreaStartY = qrBottomY;
      textY = textAreaStartY + (textAreaHeight / 2);
      
      console.log(`  QR: ${qrOffsetX}, ${qrOffsetY}, ${qrSize}x${qrSize}`);
      console.log(`  QR Bottom: ${qrBottomY}`);
      console.log(`  Text Area Start: ${textAreaStartY}`);
      console.log(`  Text Y: ${textY}`);
      
    } else if (frameType === 'top-text') {
      // QR at bottom
      qrOffsetX = offsetX + borderWidth;
      qrOffsetY = offsetY + textAreaHeight + borderWidth;
      
      const availableQRHeight = frameHeight - textAreaHeight - (borderWidth * 2);
      const availableQRWidth = frameWidth - (borderWidth * 2);
      const qrSize = Math.min(availableQRWidth, availableQRHeight);
      
      // White background for QR
      ctx.fillStyle = '#FFFFFF';
      ctx.fillRect(qrOffsetX, qrOffsetY, qrSize, qrSize);
      
      // Draw fake QR (red square for visibility)
      ctx.fillStyle = '#FF0000';
      ctx.fillRect(qrOffsetX + 50, qrOffsetY + 50, qrSize - 100, qrSize - 100);
      
      // Text position
      const textAreaStartY = offsetY;
      textY = textAreaStartY + (textAreaHeight / 2);
      
      console.log(`  QR: ${qrOffsetX}, ${qrOffsetY}, ${qrSize}x${qrSize}`);
      console.log(`  Text Area Start: ${textAreaStartY}`);
      console.log(`  Text Y: ${textY}`);
    }
    
    // Draw text
    ctx.fillStyle = '#FFFFFF';
    const fontSize = 32;
    ctx.font = `bold ${fontSize}px Arial, sans-serif`;
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    
    ctx.fillText(testText, canvasWidth / 2, textY);
    
    // Draw debug line at text Y position (green)
    ctx.strokeStyle = '#00FF00';
    ctx.lineWidth = 2;
    ctx.beginPath();
    ctx.moveTo(0, textY);
    ctx.lineTo(canvasWidth, textY);
    ctx.stroke();
    
    console.log(`  ✅ Text drawn at Y=${textY}`);
    console.log(`  Frame bounds: Y=${offsetY} to Y=${offsetY + frameHeight}`);
    console.log(`  Text is ${textY >= offsetY && textY <= offsetY + frameHeight ? 'INSIDE' : 'OUTSIDE'} frame`);
    
    // Save to file
    const buffer = canvas.toBuffer('image/png');
    const filename = `test-${frameType}.png`;
    fs.writeFileSync(filename, buffer);
    console.log(`  💾 Saved to ${filename}`);
  }
  
  console.log('\n✅ Test complete! Check test-*.png files');
}

testFrameText().catch(console.error);
