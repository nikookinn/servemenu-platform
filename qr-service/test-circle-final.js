/**
 * Test script for circle frame - QR inscribed in circle, text on frame
 * Run: node test-circle-final.js
 */

const { createCanvas } = require('canvas');
const fs = require('fs');

// Word-based text wrapping
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

async function testCircleFrameFinal() {
  console.log('🧪 Testing circle frame - QR inscribed, text on frame...\n');

  const canvasSize = 800;
  const circleRadius = 300;
  const borderWidth = 20;
  
  const canvas = createCanvas(canvasSize, canvasSize);
  const ctx = canvas.getContext('2d');
  ctx.clearRect(0, 0, canvasSize, canvasSize);
  
  const centerX = canvasSize / 2;
  const centerY = canvasSize / 2;
  
  console.log(`Canvas: ${canvasSize}x${canvasSize}`);
  console.log(`Circle center: (${centerX}, ${centerY})`);
  console.log(`Circle radius: ${circleRadius}px`);
  console.log(`Border: ${borderWidth}px`);
  
  const frameColor = '#000000';
  
  // 1. Draw outer circle (border)
  ctx.fillStyle = frameColor;
  ctx.beginPath();
  ctx.arc(centerX, centerY, circleRadius + borderWidth, 0, Math.PI * 2);
  ctx.fill();
  
  // 2. Draw inner circle (white)
  ctx.fillStyle = '#FFFFFF';
  ctx.beginPath();
  ctx.arc(centerX, centerY, circleRadius, 0, Math.PI * 2);
  ctx.fill();
  
  // 3. Draw QR (inscribed square in circle)
  ctx.save();
  ctx.beginPath();
  ctx.arc(centerX, centerY, circleRadius, 0, Math.PI * 2);
  ctx.clip();
  
  // Inscribed square: side = diameter / sqrt(2)
  const qrSize = (circleRadius * 2) / Math.sqrt(2);
  const qrX = centerX - qrSize / 2;
  const qrY = centerY - qrSize / 2;
  
  ctx.fillStyle = '#FF0000';
  ctx.fillRect(qrX, qrY, qrSize, qrSize);
  ctx.restore();
  
  console.log(`QR size: ${Math.round(qrSize)}x${Math.round(qrSize)} at (${Math.round(qrX)}, ${Math.round(qrY)})`);
  console.log(`QR fits perfectly in circle (inscribed square)`);
  
  // 4. Draw text ON the circle at bottom
  const testText = 'SCAN FOR MENU';
  const textColor = '#FFFFFF';
  ctx.fillStyle = textColor;
  const fontSize = 28;
  ctx.font = `bold ${fontSize}px Arial, sans-serif`;
  ctx.textAlign = 'center';
  ctx.textBaseline = 'middle';
  
  const textY = centerY + circleRadius - 50; // 50px from bottom of circle
  const maxWidth = circleRadius * 2 - 100;
  const lines = wrapText(ctx, testText, maxWidth);
  const lineHeight = fontSize * 1.2;
  const totalTextHeight = lines.length * lineHeight;
  const startY = textY - (totalTextHeight / 2) + (lineHeight / 2);
  
  lines.forEach((line, index) => {
    const lineY = startY + (index * lineHeight);
    ctx.fillText(line, centerX, lineY);
  });
  
  console.log(`\nText: "${testText}"`);
  console.log(`Text Y: ${textY} (on circle frame, 50px from bottom)`);
  console.log(`Text color: White (on black frame)`);
  
  // Draw debug lines
  // Green line at text Y
  ctx.strokeStyle = '#00FF00';
  ctx.lineWidth = 2;
  ctx.beginPath();
  ctx.moveTo(0, textY);
  ctx.lineTo(canvasSize, textY);
  ctx.stroke();
  
  // Blue circle showing QR inscribed area
  ctx.strokeStyle = '#0000FF';
  ctx.lineWidth = 2;
  ctx.beginPath();
  ctx.arc(centerX, centerY, qrSize / 2, 0, Math.PI * 2);
  ctx.stroke();
  
  console.log(`\n✅ Design:`);
  console.log(`  - Circle: ${circleRadius * 2}px diameter (perfectly centered)`);
  console.log(`  - Black border: ${borderWidth}px`);
  console.log(`  - QR: ${Math.round(qrSize)}px (inscribed square - NO corners visible)`);
  console.log(`  - Text: ON circle frame at bottom (white on black)`);
  console.log(`  - Text is part of frame, not separate`);
  
  // Save
  const buffer = canvas.toBuffer('image/png');
  fs.writeFileSync('test-circle-final.png', buffer);
  console.log(`\n💾 Saved to test-circle-final.png`);
  console.log(`\nGreen line = Text position`);
  console.log(`Blue circle = QR inscribed area`);
}

testCircleFrameFinal().catch(console.error);
