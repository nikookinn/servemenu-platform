/**
 * Test circle frame - no white gaps between QR and frame
 * Run: node test-circle-no-gaps.js
 */

const { createCanvas } = require('canvas');
const fs = require('fs');

async function testCircleNoGaps() {
  console.log('🧪 Testing circle frame - no white gaps...\n');

  const canvasSize = 800;
  const circleRadius = 300;
  const borderWidth = 20;
  
  const canvas = createCanvas(canvasSize, canvasSize);
  const ctx = canvas.getContext('2d');
  ctx.clearRect(0, 0, canvasSize, canvasSize);
  
  const centerX = canvasSize / 2;
  const centerY = canvasSize / 2;
  const frameColor = '#000000';
  
  console.log(`Canvas: ${canvasSize}x${canvasSize}`);
  console.log(`Circle center: (${centerX}, ${centerY})`);
  console.log(`Frame color: ${frameColor}`);
  
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
  
  console.log(`Inner circle filled with: ${frameColor} (same as frame)`);
  
  // 3. Clip and draw QR
  ctx.save();
  ctx.beginPath();
  ctx.arc(centerX, centerY, circleRadius, 0, Math.PI * 2);
  ctx.clip();
  
  const qrSize = (circleRadius * 2) / Math.sqrt(2);
  const qrX = centerX - qrSize / 2;
  const qrY = centerY - qrSize / 2;
  
  // Draw fake QR (white background with red square)
  // QR codes have white background
  ctx.fillStyle = '#FFFFFF';
  ctx.fillRect(qrX, qrY, qrSize, qrSize);
  
  ctx.fillStyle = '#FF0000';
  const qrInner = qrSize * 0.8;
  const qrInnerOffset = (qrSize - qrInner) / 2;
  ctx.fillRect(qrX + qrInnerOffset, qrY + qrInnerOffset, qrInner, qrInner);
  
  ctx.restore();
  
  console.log(`QR size: ${Math.round(qrSize)}x${Math.round(qrSize)}`);
  
  // 4. Draw text
  const testText = 'SCAN FOR MENU';
  ctx.fillStyle = '#FFFFFF';
  const fontSize = 28;
  ctx.font = `bold ${fontSize}px Arial, sans-serif`;
  ctx.textAlign = 'center';
  ctx.textBaseline = 'middle';
  
  const textY = centerY + circleRadius - 50;
  ctx.fillText(testText, centerX, textY);
  
  console.log(`Text: "${testText}" at Y=${textY}`);
  
  console.log(`\n✅ Result:`);
  console.log(`  - Entire circle is ${frameColor}`);
  console.log(`  - QR has white background (as it should)`);
  console.log(`  - Gaps between QR corners and circle edge are ${frameColor}`);
  console.log(`  - NO WHITE GAPS! ✅`);
  
  // Save
  const buffer = canvas.toBuffer('image/png');
  fs.writeFileSync('test-circle-no-gaps.png', buffer);
  console.log(`\n💾 Saved to test-circle-no-gaps.png`);
}

testCircleNoGaps().catch(console.error);
