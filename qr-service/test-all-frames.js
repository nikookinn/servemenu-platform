/**
 * Test all frame types to verify they are correct
 * Run: node test-all-frames.js
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
  return lines.slice(0, 2);
}

// 1. BOTTOM TEXT FRAME
function testBottomTextFrame() {
  console.log('\n1️⃣ BOTTOM TEXT FRAME');
  const canvasWidth = 800;
  const canvasHeight = 800;
  const frameWidth = 640;
  const frameHeight = 800;
  const borderWidth = 20;
  const textAreaHeight = 140;
  
  const canvas = createCanvas(canvasWidth, canvasHeight);
  const ctx = canvas.getContext('2d');
  ctx.clearRect(0, 0, canvasWidth, canvasHeight);
  
  const offsetX = (canvasWidth - frameWidth) / 2;
  const frameColor = '#000000';
  
  // Frame
  ctx.fillStyle = frameColor;
  ctx.fillRect(offsetX, 0, frameWidth, frameHeight);
  
  // QR area
  const qrSize = 500;
  const qrOffsetY = borderWidth + 20;
  const qrOffsetX = offsetX + (frameWidth - qrSize) / 2;
  
  ctx.fillStyle = '#FFFFFF';
  ctx.fillRect(qrOffsetX, qrOffsetY, qrSize, qrSize);
  ctx.fillStyle = '#FF0000';
  ctx.fillRect(qrOffsetX + 50, qrOffsetY + 50, qrSize - 100, qrSize - 100);
  
  // Text
  ctx.fillStyle = '#FFFFFF';
  ctx.font = 'bold 32px Arial';
  ctx.textAlign = 'center';
  ctx.textBaseline = 'middle';
  const textY = qrOffsetY + qrSize + (textAreaHeight / 2);
  ctx.fillText('SCAN FOR MENU', canvasWidth / 2, textY);
  
  fs.writeFileSync('test-frame-bottom.png', canvas.toBuffer('image/png'));
  console.log('✅ Canvas: 800x800, Frame: 640x800, QR: 500x500, Text: Bottom');
  console.log('💾 Saved: test-frame-bottom.png');
}

// 2. TOP TEXT FRAME
function testTopTextFrame() {
  console.log('\n2️⃣ TOP TEXT FRAME');
  const canvasWidth = 800;
  const canvasHeight = 800;
  const frameWidth = 640;
  const frameHeight = 800;
  const borderWidth = 20;
  const textAreaHeight = 140;
  
  const canvas = createCanvas(canvasWidth, canvasHeight);
  const ctx = canvas.getContext('2d');
  ctx.clearRect(0, 0, canvasWidth, canvasHeight);
  
  const offsetX = (canvasWidth - frameWidth) / 2;
  const frameColor = '#000000';
  
  // Frame
  ctx.fillStyle = frameColor;
  ctx.fillRect(offsetX, 0, frameWidth, frameHeight);
  
  // Text
  ctx.fillStyle = '#FFFFFF';
  ctx.font = 'bold 32px Arial';
  ctx.textAlign = 'center';
  ctx.textBaseline = 'middle';
  const textY = borderWidth + (textAreaHeight / 2);
  ctx.fillText('SCAN FOR MENU', canvasWidth / 2, textY);
  
  // QR area
  const qrSize = 500;
  const qrOffsetY = textAreaHeight + borderWidth + 20;
  const qrOffsetX = offsetX + (frameWidth - qrSize) / 2;
  
  ctx.fillStyle = '#FFFFFF';
  ctx.fillRect(qrOffsetX, qrOffsetY, qrSize, qrSize);
  ctx.fillStyle = '#FF0000';
  ctx.fillRect(qrOffsetX + 50, qrOffsetY + 50, qrSize - 100, qrSize - 100);
  
  fs.writeFileSync('test-frame-top.png', canvas.toBuffer('image/png'));
  console.log('✅ Canvas: 800x800, Frame: 640x800, QR: 500x500, Text: Top');
  console.log('💾 Saved: test-frame-top.png');
}

// 3. CIRCLE FRAME
function testCircleFrame() {
  console.log('\n3️⃣ CIRCLE FRAME');
  const canvasSize = 800;
  const circleRadius = 300;
  const borderWidth = 20;
  
  const canvas = createCanvas(canvasSize, canvasSize);
  const ctx = canvas.getContext('2d');
  ctx.clearRect(0, 0, canvasSize, canvasSize);
  
  const centerX = canvasSize / 2;
  const centerY = canvasSize / 2;
  const frameColor = '#000000';
  
  // Outer circle
  ctx.fillStyle = frameColor;
  ctx.beginPath();
  ctx.arc(centerX, centerY, circleRadius + borderWidth, 0, Math.PI * 2);
  ctx.fill();
  
  // Inner circle (frame color)
  ctx.fillStyle = frameColor;
  ctx.beginPath();
  ctx.arc(centerX, centerY, circleRadius, 0, Math.PI * 2);
  ctx.fill();
  
  // QR (clipped to circle)
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
  ctx.fillRect(qrX + 30, qrY + 30, qrSize - 60, qrSize - 60);
  ctx.restore();
  
  // Text
  ctx.fillStyle = '#FFFFFF';
  ctx.font = 'bold 24px Arial';
  ctx.textAlign = 'center';
  ctx.textBaseline = 'middle';
  const textY = centerY + circleRadius - 70;
  ctx.fillText('SCAN FOR MENU', centerX, textY);
  
  fs.writeFileSync('test-frame-circle.png', canvas.toBuffer('image/png'));
  console.log('✅ Canvas: 800x800, Circle: 600px diameter, QR: 85% inscribed, Text: On frame');
  console.log('💾 Saved: test-frame-circle.png');
}

// 4. BOX FRAME
function testBoxFrame() {
  console.log('\n4️⃣ BOX FRAME');
  const canvasSize = 800;
  const frameSize = 640;
  const borderWidth = 20;
  
  const canvas = createCanvas(canvasSize, canvasSize);
  const ctx = canvas.getContext('2d');
  ctx.clearRect(0, 0, canvasSize, canvasSize);
  
  const offsetX = (canvasSize - frameSize) / 2;
  const offsetY = (canvasSize - frameSize) / 2;
  const frameColor = '#000000';
  
  // Border frame
  ctx.fillStyle = frameColor;
  ctx.fillRect(offsetX, offsetY, frameSize, frameSize);
  
  // White background
  const qrInnerX = offsetX + borderWidth;
  const qrInnerY = offsetY + borderWidth;
  const qrInnerSize = frameSize - (borderWidth * 2);
  
  ctx.fillStyle = '#FFFFFF';
  ctx.fillRect(qrInnerX, qrInnerY, qrInnerSize, qrInnerSize);
  
  // QR
  const qrSize = qrInnerSize * 0.9;
  const qrX = qrInnerX + (qrInnerSize - qrSize) / 2;
  const qrY = qrInnerY + (qrInnerSize - qrSize) / 2;
  
  ctx.fillStyle = '#FF0000';
  ctx.fillRect(qrX, qrY, qrSize, qrSize);
  
  fs.writeFileSync('test-frame-box.png', canvas.toBuffer('image/png'));
  console.log('✅ Canvas: 800x800, Frame: 640x640, QR: 90% of inner, No text');
  console.log('💾 Saved: test-frame-box.png');
}

// 5. FRAMELESS (NEW - FULL SIZE)
function testFrameless() {
  console.log('\n5️⃣ FRAMELESS (FULL 800x800)');
  const canvasSize = 800;
  
  const canvas = createCanvas(canvasSize, canvasSize);
  const ctx = canvas.getContext('2d');
  ctx.clearRect(0, 0, canvasSize, canvasSize);
  
  // QR fills entire canvas
  ctx.fillStyle = '#FFFFFF';
  ctx.fillRect(0, 0, canvasSize, canvasSize);
  
  ctx.fillStyle = '#FF0000';
  ctx.fillRect(50, 50, canvasSize - 100, canvasSize - 100);
  
  fs.writeFileSync('test-frame-none.png', canvas.toBuffer('image/png'));
  console.log('✅ Canvas: 800x800, QR: Full 800x800 (scaled), No frame');
  console.log('💾 Saved: test-frame-none.png');
}

console.log('🧪 Testing all frame types...');
testBottomTextFrame();
testTopTextFrame();
testCircleFrame();
testBoxFrame();
testFrameless();

console.log('\n✅ All frame types tested!');
console.log('\nFrame Summary:');
console.log('1. bottom-text: 640x800 frame, QR top, text bottom');
console.log('2. top-text: 640x800 frame, text top, QR bottom');
console.log('3. circle-frame: 600px circle, QR inside (85%), text on frame');
console.log('4. box-frame: 640x640 square frame, QR only, no text');
console.log('5. none: Full 800x800, no frame');
