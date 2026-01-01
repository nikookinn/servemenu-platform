/**
 * Test circle frame with dynamic font sizing
 * Run: node test-circle-dynamic-text.js
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

function drawCircleFrame(testText, filename) {
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
  
  const inscribedSquare = (circleRadius * 2) / Math.sqrt(2);
  const qrSize = inscribedSquare * 0.85;
  const qrX = centerX - qrSize / 2;
  const qrY = centerY - qrSize / 2;
  
  ctx.fillStyle = '#FFFFFF';
  ctx.fillRect(qrX, qrY, qrSize, qrSize);
  
  ctx.fillStyle = '#FF0000';
  const qrInner = qrSize * 0.8;
  const qrInnerOffset = (qrSize - qrInner) / 2;
  ctx.fillRect(qrX + qrInnerOffset, qrY + qrInnerOffset, qrInner, qrInner);
  ctx.restore();
  
  // Dynamic font size
  const textLength = testText.length;
  let fontSize;
  if (textLength > 50) {
    fontSize = 18; // Small for very long text
  } else if (textLength > 30) {
    fontSize = 20; // Medium for long text
  } else {
    fontSize = 24; // Normal for short text
  }
  
  ctx.fillStyle = '#FFFFFF';
  ctx.font = `bold ${fontSize}px Arial, sans-serif`;
  ctx.textAlign = 'center';
  ctx.textBaseline = 'middle';
  
  const textY = centerY + circleRadius - 70;
  const maxWidth = circleRadius * 2 - 140;
  const lines = wrapText(ctx, testText, maxWidth);
  const lineHeight = fontSize * 1.15;
  const totalTextHeight = lines.length * lineHeight;
  const startY = textY - (totalTextHeight / 2) + (lineHeight / 2);
  
  lines.forEach((line, i) => {
    const lineY = startY + (i * lineHeight);
    ctx.fillText(line, centerX, lineY);
  });
  
  // Save
  const buffer = canvas.toBuffer('image/png');
  fs.writeFileSync(filename, buffer);
  
  return {
    textLength,
    fontSize,
    lines: lines.length,
    lineTexts: lines
  };
}

console.log('🧪 Testing circle frame with dynamic font sizing...\n');

const tests = [
  { text: 'SCAN FOR MENU', name: 'short' },
  { text: 'SCAN THIS QR CODE TO VIEW OUR MENU', name: 'medium' },
  { text: 'CAN YOU GIVE ME YOUR NUMBER FOR FUN WHILE I AM ATT', name: 'long' },
  { text: 'WELCOME TO OUR RESTAURANT PLEASE SCAN THIS CODE TO VIEW OUR COMPLETE MENU', name: 'very-long' }
];

tests.forEach((test, index) => {
  const result = drawCircleFrame(test.text, `test-dynamic-${test.name}.png`);
  
  console.log(`--- Test ${index + 1}: ${test.name.toUpperCase()} (${result.textLength} chars) ---`);
  console.log(`Text: "${test.text}"`);
  console.log(`Font size: ${result.fontSize}px`);
  console.log(`Lines: ${result.lines}`);
  result.lineTexts.forEach((line, i) => {
    console.log(`  Line ${i + 1}: "${line}"`);
  });
  console.log(`💾 Saved to test-dynamic-${test.name}.png\n`);
});

console.log('✅ Dynamic Font Sizing:');
console.log('  - 0-30 chars: 24px (normal)');
console.log('  - 31-50 chars: 20px (medium)');
console.log('  - 51+ chars: 18px (small)');
console.log('  - Max width: 460px (safe margin)');
console.log('  - Text position: 70px from bottom');
console.log('  - Line spacing: 1.15x (tighter)');
