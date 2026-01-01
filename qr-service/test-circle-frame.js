/**
 * Test script for circle frame with text wrapping
 * Run: node test-circle-frame.js
 */

const { createCanvas } = require('canvas');
const fs = require('fs');

// Word-based text wrapping function
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

async function testCircleFrame() {
  console.log('🧪 Testing circle frame with text wrapping...\n');

  const canvasSize = 800;
  const circleRadius = 280;
  const borderWidth = 20;
  const textAreaHeight = 120;
  const textTopMargin = 30;
  
  const canvas = createCanvas(canvasSize, canvasSize);
  const ctx = canvas.getContext('2d');
  ctx.clearRect(0, 0, canvasSize, canvasSize);
  
  const centerX = canvasSize / 2;
  const totalHeight = (circleRadius + borderWidth) * 2 + textTopMargin + textAreaHeight;
  const centerY = (canvasSize - totalHeight) / 2 + circleRadius + borderWidth;
  
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
  
  // 3. Draw fake QR (red square in circle)
  ctx.save();
  ctx.beginPath();
  ctx.arc(centerX, centerY, circleRadius, 0, Math.PI * 2);
  ctx.clip();
  
  const qrSize = circleRadius * 2 * 0.85;
  const qrX = centerX - qrSize / 2;
  const qrY = centerY - qrSize / 2;
  
  ctx.fillStyle = '#FF0000';
  ctx.fillRect(qrX, qrY, qrSize, qrSize);
  ctx.restore();
  
  console.log(`QR size: ${qrSize}x${qrSize} at (${qrX}, ${qrY})`);
  
  // 4. Draw text with wrapping
  const testTexts = [
    'SCAN FOR MENU',
    'SCAN THIS QR CODE TO VIEW OUR MENU',
    'WELCOME TO OUR RESTAURANT PLEASE SCAN THIS CODE'
  ];
  
  testTexts.forEach((testText, index) => {
    console.log(`\n--- Test ${index + 1}: "${testText}" ---`);
    
    const textColor = '#000000';
    ctx.fillStyle = textColor;
    const fontSize = 28;
    ctx.font = `bold ${fontSize}px Arial, sans-serif`;
    ctx.textAlign = 'center';
    ctx.textBaseline = 'top';
    
    const textStartY = centerY + circleRadius + borderWidth + textTopMargin;
    const maxWidth = canvasSize - 120;
    const lines = wrapText(ctx, testText, maxWidth);
    const lineHeight = fontSize * 1.3;
    
    console.log(`Text starts at Y: ${textStartY}`);
    console.log(`Max width: ${maxWidth}px`);
    console.log(`Lines: ${lines.length}`);
    lines.forEach((line, i) => {
      console.log(`  Line ${i + 1}: "${line}"`);
    });
    
    // Clear previous text
    ctx.fillStyle = '#FFFFFF';
    ctx.fillRect(0, textStartY - 10, canvasSize, textAreaHeight + 20);
    
    // Draw new text
    ctx.fillStyle = textColor;
    lines.forEach((line, i) => {
      const lineY = textStartY + (i * lineHeight);
      ctx.fillText(line, centerX, lineY);
    });
    
    // Draw debug line at text start
    ctx.strokeStyle = '#00FF00';
    ctx.lineWidth = 2;
    ctx.beginPath();
    ctx.moveTo(0, textStartY);
    ctx.lineTo(canvasSize, textStartY);
    ctx.stroke();
    
    // Save each test
    const buffer = canvas.toBuffer('image/png');
    fs.writeFileSync(`test-circle-${index + 1}.png`, buffer);
    console.log(`💾 Saved to test-circle-${index + 1}.png`);
  });
  
  console.log(`\n✅ Design:`);
  console.log(`  - Circle: ${circleRadius * 2}px diameter (centered)`);
  console.log(`  - Black border: ${borderWidth}px`);
  console.log(`  - QR: 85% of circle diameter`);
  console.log(`  - Text: Below circle with ${textTopMargin}px margin`);
  console.log(`  - Word-based wrapping for long text`);
  console.log(`\nGreen line = Text start position`);
}

testCircleFrame().catch(console.error);
