/**
 * Test circle frame with balanced QR and text sizes
 * Run: node test-circle-balanced.js
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

async function testCircleBalanced() {
  console.log('🧪 Testing circle frame with balanced QR and text...\n');

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
  
  // Draw QR (smaller - 85% of inscribed square)
  ctx.save();
  ctx.beginPath();
  ctx.arc(centerX, centerY, circleRadius, 0, Math.PI * 2);
  ctx.clip();
  
  const inscribedSquare = (circleRadius * 2) / Math.sqrt(2);
  const qrSize = inscribedSquare * 0.85; // 85% of inscribed
  const qrX = centerX - qrSize / 2;
  const qrY = centerY - qrSize / 2;
  
  ctx.fillStyle = '#FFFFFF';
  ctx.fillRect(qrX, qrY, qrSize, qrSize);
  
  ctx.fillStyle = '#FF0000';
  const qrInner = qrSize * 0.8;
  const qrInnerOffset = (qrSize - qrInner) / 2;
  ctx.fillRect(qrX + qrInnerOffset, qrY + qrInnerOffset, qrInner, qrInner);
  ctx.restore();
  
  console.log(`Circle radius: ${circleRadius}px`);
  console.log(`Inscribed square: ${Math.round(inscribedSquare)}px`);
  console.log(`QR size: ${Math.round(qrSize)}px (85% of inscribed)`);
  console.log(`QR reduction: ${Math.round(inscribedSquare - qrSize)}px smaller`);
  
  // Draw text (larger - 24px)
  const testText = 'SCAN FOR MENU';
  ctx.fillStyle = '#FFFFFF';
  const fontSize = 24;
  ctx.font = `bold ${fontSize}px Arial, sans-serif`;
  ctx.textAlign = 'center';
  ctx.textBaseline = 'middle';
  
  const textY = centerY + circleRadius - 45;
  const maxWidth = circleRadius * 2 - 120;
  const lines = wrapText(ctx, testText, maxWidth);
  const lineHeight = fontSize * 1.2;
  const totalTextHeight = lines.length * lineHeight;
  const startY = textY - (totalTextHeight / 2) + (lineHeight / 2);
  
  lines.forEach((line, i) => {
    const lineY = startY + (i * lineHeight);
    ctx.fillText(line, centerX, lineY);
  });
  
  console.log(`\nText: "${testText}"`);
  console.log(`Font size: ${fontSize}px (was 22px)`);
  console.log(`Text Y: ${textY} (45px from bottom)`);
  console.log(`Max width: ${maxWidth}px`);
  
  // Draw guide lines
  // QR boundary (blue)
  ctx.strokeStyle = '#0000FF';
  ctx.lineWidth = 2;
  ctx.strokeRect(qrX, qrY, qrSize, qrSize);
  
  // Text baseline (green)
  ctx.strokeStyle = '#00FF00';
  ctx.beginPath();
  ctx.moveTo(0, textY);
  ctx.lineTo(canvasSize, textY);
  ctx.stroke();
  
  console.log(`\n✅ Balance:`);
  console.log(`  - QR: Smaller (85% of max) - more breathing room`);
  console.log(`  - Text: Larger (24px vs 22px) - more readable`);
  console.log(`  - Better visual balance! ✅`);
  
  // Save
  const buffer = canvas.toBuffer('image/png');
  fs.writeFileSync('test-circle-balanced.png', buffer);
  console.log(`\n💾 Saved to test-circle-balanced.png`);
  console.log(`\nBlue box = QR boundary`);
  console.log(`Green line = Text baseline`);
}

testCircleBalanced().catch(console.error);
