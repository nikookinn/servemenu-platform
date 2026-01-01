/**
 * Test circle frame with smaller text
 * Run: node test-circle-smaller-text.js
 */

const { createCanvas } = require('canvas');
const fs = require('fs');

function wrapText(ctx, text, maxWidth) {
  const words = text.split(' ');
  const lines = [];
  let currentLine = words[0];

  for (let i = 1; i < words.length; i++) {
    const word = words[i];
    const width = ctx.measureText(currentLine + ' ' + word).width;
    if (width < maxWidth) {
      currentLine += ' ' + word;
    } else {
      lines.push(currentLine);
      currentLine = word;
    }
  }
  lines.push(currentLine);
  return lines;
}

async function testCircleSmallerText() {
  console.log('🧪 Testing circle frame with smaller text...\n');

  const canvasSize = 800;
  const circleRadius = 300;
  const borderWidth = 20;
  
  const canvas = createCanvas(canvasSize, canvasSize);
  const ctx = canvas.getContext('2d');
  ctx.clearRect(0, 0, canvasSize, canvasSize);
  
  const centerX = canvasSize / 2;
  const centerY = canvasSize / 2;
  const frameColor = '#000000';
  
  // Draw circles
  ctx.fillStyle = frameColor;
  ctx.beginPath();
  ctx.arc(centerX, centerY, circleRadius + borderWidth, 0, Math.PI * 2);
  ctx.fill();
  
  ctx.fillStyle = frameColor;
  ctx.beginPath();
  ctx.arc(centerX, centerY, circleRadius, 0, Math.PI * 2);
  ctx.fill();
  
  // Draw QR
  ctx.save();
  ctx.beginPath();
  ctx.arc(centerX, centerY, circleRadius, 0, Math.PI * 2);
  ctx.clip();
  
  const qrSize = (circleRadius * 2) / Math.sqrt(2);
  const qrX = centerX - qrSize / 2;
  const qrY = centerY - qrSize / 2;
  
  ctx.fillStyle = '#FFFFFF';
  ctx.fillRect(qrX, qrY, qrSize, qrSize);
  
  ctx.fillStyle = '#FF0000';
  const qrInner = qrSize * 0.8;
  const qrInnerOffset = (qrSize - qrInner) / 2;
  ctx.fillRect(qrX + qrInnerOffset, qrY + qrInnerOffset, qrInner, qrInner);
  ctx.restore();
  
  // Test different texts
  const testTexts = [
    'SCAN FOR MENU',
    'SCAN THIS QR CODE',
    'WELCOME TO OUR RESTAURANT'
  ];
  
  testTexts.forEach((testText, index) => {
    console.log(`\n--- Test ${index + 1}: "${testText}" ---`);
    
    // Clear previous text area
    ctx.fillStyle = frameColor;
    ctx.beginPath();
    ctx.arc(centerX, centerY, circleRadius, 0, Math.PI * 2);
    ctx.fill();
    
    // Redraw QR
    ctx.save();
    ctx.beginPath();
    ctx.arc(centerX, centerY, circleRadius, 0, Math.PI * 2);
    ctx.clip();
    ctx.fillStyle = '#FFFFFF';
    ctx.fillRect(qrX, qrY, qrSize, qrSize);
    ctx.fillStyle = '#FF0000';
    ctx.fillRect(qrX + qrInnerOffset, qrY + qrInnerOffset, qrInner, qrInner);
    ctx.restore();
    
    // Draw text
    ctx.fillStyle = '#FFFFFF';
    const fontSize = 22; // Smaller font
    ctx.font = `bold ${fontSize}px Arial, sans-serif`;
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    
    const textY = centerY + circleRadius - 40; // 40px from bottom
    const maxWidth = circleRadius * 2 - 140; // More margin
    const lines = wrapText(ctx, testText, maxWidth);
    const lineHeight = fontSize * 1.2;
    const totalTextHeight = lines.length * lineHeight;
    const startY = textY - (totalTextHeight / 2) + (lineHeight / 2);
    
    lines.forEach((line, i) => {
      const lineY = startY + (i * lineHeight);
      ctx.fillText(line, centerX, lineY);
    });
    
    console.log(`Font size: ${fontSize}px`);
    console.log(`Max width: ${maxWidth}px`);
    console.log(`Text Y: ${textY}`);
    console.log(`Lines: ${lines.length}`);
    lines.forEach((line, i) => console.log(`  Line ${i + 1}: "${line}"`));
    
    // Save
    const buffer = canvas.toBuffer('image/png');
    fs.writeFileSync(`test-circle-text-${index + 1}.png`, buffer);
    console.log(`💾 Saved to test-circle-text-${index + 1}.png`);
  });
  
  console.log(`\n✅ Changes:`);
  console.log(`  - Font size: 28px → 22px (smaller)`);
  console.log(`  - Text Y: 50px → 40px from bottom (closer to bottom)`);
  console.log(`  - Max width: 500px → 460px (more margin for circle curve)`);
  console.log(`  - Text fits better on circle frame! ✅`);
}

testCircleSmallerText().catch(console.error);
