/**
 * Test QR eye types
 * Run: node test-eye-types.js
 */

const { createCanvas, loadImage } = require('canvas');
const { JSDOM } = require('jsdom');
const fs = require('fs');

const { QRCodeStyling } = require('qr-code-styling/lib/qr-code-styling.common.js');

async function testEyeType(eyeType, filename) {
  console.log(`\n🧪 Testing eye type: ${eyeType}`);
  
  const qrCode = new QRCodeStyling({
    jsdom: JSDOM,
    nodeCanvas: { createCanvas, loadImage },
    width: 400,
    height: 400,
    data: 'https://example.com',
    margin: 1,
    qrOptions: {
      errorCorrectionLevel: 'M'
    },
    backgroundOptions: {
      color: '#FFFFFF'
    },
    dotsOptions: {
      type: 'rounded',
      color: '#000000'
    },
    cornersSquareOptions: {
      type: eyeType,
      color: '#000000'
    },
    cornersDotOptions: {
      type: eyeType,
      color: '#000000'
    }
  });

  const buffer = await qrCode.getRawData('png');
  fs.writeFileSync(filename, buffer);
  console.log(`✅ Saved: ${filename}`);
}

async function main() {
  console.log('🎨 Testing QR Eye Types...\n');
  
  await testEyeType('square', 'test-eye-square.png');
  await testEyeType('dot', 'test-eye-dot.png');
  await testEyeType('extra-rounded', 'test-eye-extra-rounded.png');
  
  console.log('\n✅ All eye types tested!');
  console.log('\nEye Types:');
  console.log('1. square: Sharp corners (default)');
  console.log('2. dot: Fully rounded (circles)');
  console.log('3. extra-rounded: Rounded corners');
}

main().catch(console.error);
