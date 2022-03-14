import FeatureInterface from 'core/objects/FeatureInterface';
import React from 'react';

export interface ConnectConfiguration {}

export const CONNECT_FEATURE_IDENTIFIER = 'connect';

/**
 * The interface the evoting feature exposes
 */
export interface ConnectInterface extends FeatureInterface {
  navigation: {
    ConnectNavigation: React.ComponentType<unknown>;
  };
  functions: {
    /**
     * Given the lao server address and the lao id, this computes the data
     * that is encoded in a QR code that can be used to connect to a LAO
     */
    encodeLaoConnectionInQRCode: (server: string, laoId: string) => string;
  };
}
