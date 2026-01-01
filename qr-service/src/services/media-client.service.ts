/**
 * Media Client Service for QR Service
 * Fetches media (logos) from Media Service via HTTP
 */

import axios from 'axios';
import { logger } from '../utils/logger';
import { keycloakAuthService } from './keycloak-auth.service';

interface MediaAssetResponse {
  id: string;
  entityId: string;
  mediaType: string;
  originalFilename: string;
  contentType: string;
  fileSize: number;
  s3Key: string;
  thumbnailUrl?: string;
  mediumUrl?: string;
  largeUrl?: string;
  uploadedBy?: string;
  createdAt: string;
  updatedAt: string;
}

export class MediaClientService {
  private apiGatewayUrl: string;

  constructor() {
    this.apiGatewayUrl = process.env.API_GATEWAY_URL || 'http://localhost:8080';
  }

  /**
   * Fetch logo image from Media Service by mediaId
   * Returns medium size (256x256) for QR embedding
   * 
   * @param mediaId Media asset ID (UUID)
   * @returns Logo URL (medium size) or null if not found
   */
  async fetchLogoUrl(mediaId: string): Promise<string | null> {
    try {
      logger.info('🔍 Fetching logo from Media Service', { mediaId });

      // Get OAuth2 access token from Keycloak
      const accessToken = await keycloakAuthService.getAccessToken();

      // Fetch media asset metadata
      const response = await axios.get<MediaAssetResponse>(
        `${this.apiGatewayUrl}/media-service/api/v1/media/${mediaId}`,
        {
          headers: {
            'Authorization': `Bearer ${accessToken}`
          },
          timeout: 5000
        }
      );

      const media = response.data;

      // Use medium size (256x256) for QR embedding
      const logoUrl = media.mediumUrl;

      if (!logoUrl) {
        logger.warn('⚠️ Logo URL not found in media response', { mediaId });
        return null;
      }

      logger.info('✅ Logo URL fetched successfully', { 
        mediaId, 
        logoUrl,
        size: 'medium (256x256)'
      });

      return logoUrl;

    } catch (error) {
      if (axios.isAxiosError(error)) {
        logger.error('❌ Failed to fetch logo from Media Service', {
          mediaId,
          status: error.response?.status,
          message: error.message
        });
      } else {
        logger.error('❌ Unexpected error fetching logo', {
          mediaId,
          error: error instanceof Error ? error.message : error
        });
      }
      return null; // Graceful degradation - QR will generate without logo
    }
  }

  /**
   * Download logo image bytes from URL
   * Used to embed logo directly in QR code
   * 
   * @param logoUrl Logo URL from Media Service
   * @returns Image buffer or null if download fails
   */
  async downloadLogoImage(logoUrl: string): Promise<Buffer | null> {
    try {
      logger.debug('📥 Downloading logo image', { logoUrl });

      const response = await axios.get(logoUrl, {
        responseType: 'arraybuffer',
        timeout: 10000
      });

      const buffer = Buffer.from(response.data);

      logger.debug('✅ Logo image downloaded', { 
        size: buffer.length,
        sizeKB: (buffer.length / 1024).toFixed(2)
      });

      return buffer;

    } catch (error) {
      logger.error('❌ Failed to download logo image', {
        logoUrl,
        error: error instanceof Error ? error.message : error
      });
      return null;
    }
  }
}

export const mediaClientService = new MediaClientService();
