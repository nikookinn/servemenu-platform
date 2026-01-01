/**
 * Keycloak Authentication Service for Service-to-Service Communication
 * 
 * Uses OAuth2 Client Credentials Flow to obtain access tokens
 * This is the PROPER way to authenticate microservices with Keycloak
 * 
 * Flow:
 * 1. QR Service requests token from Keycloak using client_id + client_secret
 * 2. Keycloak returns JWT access token
 * 3. QR Service includes token in Authorization header when calling Business Service
 * 4. Business Service validates token with Keycloak
 */

import axios from 'axios';
import { logger } from '../utils/logger';

interface TokenResponse {
  access_token: string;
  expires_in: number;
  refresh_expires_in: number;
  token_type: string;
  scope: string;
}

export class KeycloakAuthService {
  private keycloakUrl: string;
  private realm: string;
  private clientId: string;
  private clientSecret: string;
  private cachedToken: string | null = null;
  private tokenExpiry: number = 0;

  constructor() {
    this.keycloakUrl = process.env.KEYCLOAK_URL || 'http://localhost:8081';
    this.realm = process.env.KEYCLOAK_REALM || 'servemenu-platform';
    this.clientId = process.env.KEYCLOAK_CLIENT_ID || 'qr-service';
    this.clientSecret = process.env.KEYCLOAK_CLIENT_SECRET || '';

    if (!this.clientSecret) {
      logger.warn('⚠️ KEYCLOAK_CLIENT_SECRET not set - service-to-service auth will fail');
    }
  }

  /**
   * Get access token for service-to-service communication
   * Uses cached token if still valid, otherwise requests new token
   */
  async getAccessToken(): Promise<string> {
    // Return cached token if still valid (with 30 second buffer)
    const now = Date.now();
    if (this.cachedToken && this.tokenExpiry > now + 30000) {
      logger.debug('✅ Using cached Keycloak token');
      return this.cachedToken;
    }

    // Request new token using Client Credentials Flow
    try {
      logger.debug('🔑 Requesting new Keycloak token', {
        realm: this.realm,
        clientId: this.clientId
      });

      const tokenUrl = `${this.keycloakUrl}/realms/${this.realm}/protocol/openid-connect/token`;

      const params = new URLSearchParams();
      params.append('grant_type', 'client_credentials');
      params.append('client_id', this.clientId);
      params.append('client_secret', this.clientSecret);

      const response = await axios.post<TokenResponse>(tokenUrl, params, {
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded'
        },
        timeout: 5000
      });

      this.cachedToken = response.data.access_token;
      this.tokenExpiry = now + (response.data.expires_in * 1000);

      logger.info('✅ Keycloak token obtained', {
        expiresIn: response.data.expires_in,
        tokenType: response.data.token_type,
        scope: response.data.scope
      });

      return this.cachedToken;

    } catch (error) {
      logger.error('❌ Failed to obtain Keycloak token', {
        error: error instanceof Error ? {
          message: error.message,
          stack: error.stack
        } : error,
        keycloakUrl: this.keycloakUrl,
        realm: this.realm,
        clientId: this.clientId
      });
      throw new Error('Failed to authenticate with Keycloak');
    }
  }

  /**
   * Clear cached token (useful for testing or forcing token refresh)
   */
  clearCache(): void {
    this.cachedToken = null;
    this.tokenExpiry = 0;
    logger.debug('🗑️ Keycloak token cache cleared');
  }
}

export const keycloakAuthService = new KeycloakAuthService();
