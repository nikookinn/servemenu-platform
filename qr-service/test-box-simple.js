/**
 * Test script for simple box frame (QR only, no text)
 * Run: node test-box-simple.js
 */

const { createCanvas } = require('canvas');
const fs = require('fs');

async function testSimpleBoxFrame() {
  console.log('🧪 Testing simple box frame (QR only)...\n');

  const canvasSize = 800;
  const frameSize = 640;
  const borderWidth = 20;
  
  const canvas = createCanvas(canvasSize, canvasSize);
  const ctx = canvas.getContext('2d');
  ctx.clearRect(0, 0, canvasSize, canvasSize);
  
  const offsetX = (canvasSize - frameSize) / 2;
  const offsetY = (canvasSize - frameSize) / 2;
  const frameColor = '#000000';
  
  console.log(`Canvas: ${canvasSize}x${canvasSize}`);
  console.log(`Frame: ${frameSize}x${frameSize} at (${offsetX}, ${offsetY})`);
  console.log(`Border: ${borderWidth}px`);
  
  // Draw border frame
  ctx.fillStyle = frameColor;
  ctx.fillRect(offsetX, offsetY, frameSize, frameSize);
  
  // Draw white background for QR
  const qrInnerX = offsetX + borderWidth;
  const qrInnerY = offsetY + borderWidth;
  const qrInnerSize = frameSize - (borderWidth * 2);
  
  ctx.fillStyle = '#FFFFFF';
  ctx.fillRect(qrInnerX, qrInnerY, qrInnerSize, qrInnerSize);
  
  console.log(`White area: ${qrInnerSize}x${qrInnerSize} at (${qrInnerX}, ${qrInnerY})`);
  
  // Draw fake QR (red square)
  const qrSize = qrInnerSize * 0.9;
  const qrX = qrInnerX + (qrInnerSize - qrSize) / 2;
  const qrY = qrInnerY + (qrInnerSize - qrSize) / 2;
  
  ctx.fillStyle = '#FF0000';
  ctx.fillRect(qrX, qrY, qrSize, qrSize);
  
  console.log(`QR Code: ${qrSize}x${qrSize} at (${qrX}, ${qrY})`);
  
  console.log(`\n✅ Design:`);
  console.log(`  - Square frame: ${frameSize}x${frameSize}px`);
  console.log(`  - Black border: ${borderWidth}px`);
  console.log(`  - White background for QR`);
  console.log(`  - QR centered (90% of white area)`);
  console.log(`  - NO TEXT AREA`);
  
  // Save to file
  const buffer = canvas.toBuffer('image/png');
  fs.writeFileSync('test-box-simple.png', buffer);
  console.log(`\n💾 Saved to test-box-simple.png`);
}

testSimpleBoxFrame().catch(console.error);
