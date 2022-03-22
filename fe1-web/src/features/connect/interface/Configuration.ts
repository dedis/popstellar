import React from 'react';
import { AnyAction } from 'redux';

import { Channel, Hash } from 'core/objects';
import FeatureInterface from 'core/objects/FeatureInterface';

export const CONNECT_FEATURE_IDENTIFIER = 'connect';

export interface ConnectConfiguration {
  /**
   * A function for adding a lao server address
   * @param laoId The lao id
   * @param address The address that should be added
   * @returns A redux action
   */
  addLaoServerAddress: (laoId: Hash | string, address: string) => AnyAction;

  /**
   * A function for getting a LAOs channel by its id
   * @param laoId The id of the lao whose channel should be returned
   * @returns The channel related to the passed lao id or undefined it the lao id is invalid
   */
  getLaoChannel(laoId: string): Channel;

  /**
   * A hook returning the current lao id
   * @returns The current lao id
   */
  useCurrentLaoId: () => Hash | undefined;
}

export type ConnectReactContext = Pick<
  ConnectConfiguration,
  'addLaoServerAddress' | 'getLaoChannel' | 'useCurrentLaoId'
>;

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
     * @param server The server address
     * @param laoId The lao id
     * @returns The encoded data
     */
    encodeLaoConnectionForQRCode: (server: string, laoId: string) => string;
  };
  context: ConnectReactContext;
}
