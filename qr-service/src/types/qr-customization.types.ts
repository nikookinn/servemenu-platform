/**
 * QR Customization Types
 * MUST match frontend types exactly for consistency
 * Source: front-end/servemenu-platform-react/src/features/store/types/store.types.ts
 */

export type QRPatternType = 'square' | 'dots' | 'rounded' | 'extra-rounded' | 'classy' | 'classy-rounded';
export type QREyeType = 'square' | 'dot' | 'rounded' | 'extra-rounded';
export type QRFrameType = 'none' | 'bottom-text' | 'top-text' | 'box-frame' | 'circle-frame';
export type QRPatternColorMode = 'single' | 'gradient';
export type QRGradientType = 'linear' | 'radial';

export interface QRCustomization {
  id: string;
  storeId: string;
  backgroundColor: string;
  patternColorMode: QRPatternColorMode;
  patternColorSingle: string;
  patternGradientType?: QRGradientType;
  patternGradientStart?: string;
  patternGradientEnd?: string;
  patternGradientRotation?: number;
  patternType: QRPatternType;
  eyeType: QREyeType;
  eyeColorEnabled: boolean;
  eyeColorOuter?: string;
  eyeColorInner?: string;
  frameType: QRFrameType;
  frameText?: string;
  frameTextColor?: string;
  frameColorSingle?: string;
  frameFont?: string;
  logoMediaId?: string;
  logoUrl?: string;
  logoSize?: number;
  margin?: number;
  createdAt: string;
  updatedAt: string;
}

export interface TableCreatedEvent {
  eventId: string;
  eventType: 'TABLE_CREATED';
  timestamp: string;
  aggregateId: string; // tableId
  payload: {
    tableId: string;
    storeId: string;
    businessId: string;
    tableName: string;
    tableNumber: string;
    qrUrl: string; // The URL that should be encoded in QR
  };
}

export interface QRGeneratedEvent {
  eventId: string;
  eventType: 'QR_GENERATED';
  timestamp: string;
  aggregateId: string; // tableId
  payload: {
    tableId: string;
    storeId: string;
    qrImageBase64: string; // PNG image as base64
    qrImageSize: number; // File size in bytes
    qrUrl: string; // The encoded URL
  };
}
