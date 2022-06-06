import React from 'react';
import { AnyAction } from 'redux';

import { AppScreen } from 'core/navigation/AppNavigation';
import { Channel, Hash } from 'core/objects';
import FeatureInterface from 'core/objects/FeatureInterface';

import { HomeFeature } from './Feature';

export const HOME_FEATURE_IDENTIFIER = 'home';

export interface HomeCompositionConfiguration {
  /* lao */

  /**
   * A function for getting a LAOs channel by its id
   * @param laoId The id of the lao whose channel should be returned
   * @returns The channel related to the passed lao id or undefined it the lao id is invalid
   */
  getLaoChannel(laoId: string): Channel | undefined;

  /**
   * A hook returning the current lao id
   * @returns The current lao id
   */
  useCurrentLaoId: () => Hash | undefined;

  /* functions */
  requestCreateLao: (laoName: string) => Promise<Channel>;
  connectToTestLao: () => void;

  /* action creators */
  /**
   * A function for adding a lao server address
   * @param laoId The lao id
   * @param address The address that should be added
   * @returns A redux action
   */
  addLaoServerAddress: (laoId: Hash | string, address: string) => AnyAction;

  /* hooks */

  /**
   * Gets the current lao list
   * @returns The current lao list
   */
  useLaoList: () => HomeFeature.Lao[];

  /* components */

  /**
   * A component rendering the list of recent laos
   */
  LaoList: React.ComponentType<unknown>;

  /* other */

  /**
   * A list of screens show in the main navigations
   */
  homeNavigationScreens: HomeFeature.HomeScreen[];
}

/**
 * The type of the context that is provided to react home components
 */
export type HomeReactContext = Pick<
  HomeCompositionConfiguration,
  /* lao */
  | 'requestCreateLao'
  | 'addLaoServerAddress'
  | 'connectToTestLao'
  | 'useLaoList'
  | 'LaoList'
  | 'homeNavigationScreens'
  | 'getLaoChannel'
  | 'useCurrentLaoId'
>;

/**
 * The interface the home feature exposes
 */
export interface HomeInterface extends FeatureInterface {
  appScreens: AppScreen[];
  functions: {
    /**
     * Given the lao server address and the lao id, this computes the data
     * that is encoded in a QR code that can be used to connect to a LAO
     * @param servers The server addresses
     * @param laoId The lao id
     * @returns The encoded data
     */
    encodeLaoConnectionForQRCode: (servers: string[], laoId: string) => string;
  };
  context: HomeReactContext;
}
