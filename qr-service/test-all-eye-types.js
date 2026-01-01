/**
 * Test all QR eye types
 * Run: node test-all-eye-types.js
 */

const { createCanvas, loadImage } = require('canvas');
const { JSDOM } = require('jsdom');
const fs = require('fs');

const { QRCodeStyling } = require('qr-code-styling/lib/qr-code-styling.common.js');

async function testEyeType(eyeType, filename, label) {
  console.log(`\n🧪 Testing: ${label} (${eyeType})`);
  
  try {
    const qrCode = new QRCodeStyling({
      jsdom: JSDOM,
      nodeCanvas: { createCanvas, loadImage },
      width: 400,
      height: 400,
      data: 'https://example.com/test',
      margin: 2,
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
        color: '#FF0000' // Red for visibility
      },
      cornersDotOptions: {
        type: eyeType,
        color: '#0000FF' // Blue for visibility
      }
    });

    const buffer = await qrCode.getRawData('png');
    fs.writeFileSync(filename, buffer);
    console.log(`✅ Success: ${filename}`);
    return true;
  } catch (error) {
    console.log(`❌ Failed: ${error.message}`);
    return false;
  }
}

async function main() {
  console.log('🎨 Testing All QR Eye Types...\n');
  console.log('Testing 4 eye types: Square, Dot, Rounded, Extra Rounded\n');
  
  const tests = [
    { type: 'square', label: 'Square (Sharp corners)', file: 'test-eye-square-all.png' },
    { type: 'dot', label: 'Dot (Full circles)', file: 'test-eye-dot-all.png' },
    { type: 'rounded', label: 'Rounded (Should map to dot)', file: 'test-eye-rounded-all.png' },
    { type: 'extra-rounded', label: 'Extra Rounded (Rounded corners)', file: 'test-eye-extra-rounded-all.png' }
  ];
  
  const results = [];
  
  for (const test of tests) {
    const success = await testEyeType(test.type, test.file, test.label);
    results.push({ ...test, success });
  }
  
  console.log('\n' + '='.repeat(60));
  console.log('📊 RESULTS:');
  console.log('='.repeat(60));
  
  results.forEach((result, index) => {
    const status = result.success ? '✅ WORKING' : '❌ FAILED';
    console.log(`${index + 1}. ${result.label}`);
    console.log(`   Type: "${result.type}" → ${status}`);
    console.log(`   File: ${result.file}`);
    console.log('');
  });
  
  const allWorking = results.every(r => r.success);
  
  if (allWorking) {
    console.log('🎉 ALL EYE TYPES WORKING!');
  } else {
    console.log('⚠️  Some eye types failed!');
  }
  
  console.log('\n' + '='.repeat(60));
  console.log('Eye Type Mapping in QR Service:');
  console.log('- square → square (sharp corners)');
  console.log('- dot → dot (full circles)');
  console.log('- rounded → dot (alias for dot)');
  console.log('- extra-rounded → extra-rounded (rounded corners)');
  console.log('='.repeat(60));
}

main().catch(console.error);
