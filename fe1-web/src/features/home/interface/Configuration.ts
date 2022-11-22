import React from 'react';
import { AnyAction, Dispatch } from 'redux';

import { AppScreen } from 'core/navigation/AppNavigation';
import { NetworkConnection } from 'core/network/NetworkConnection';
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
  getLaoChannel(laoId: Hash): Channel | undefined;

  /**
   * Gets a lao from the store by its id
   * @param laoId The id of the lao
   * @returns A lao or undefined if none was found
   */
  getLaoById(laoId: Hash): HomeFeature.Lao | undefined;

  /**
   * A hook returning the current lao id
   * @returns The current lao id
   */
  useCurrentLaoId: () => Hash | undefined;

  /* functions */

  /**
   * Sends a request to create a new lao
   */
  requestCreateLao: (laoName: string) => Promise<Channel>;

  connectToTestLao: () => void;

  /**
   * Resubscribes to a known lao
   */
  resubscribeToLao: (
    lao: HomeFeature.Lao,
    dispatch: Dispatch,
    connections?: NetworkConnection[],
  ) => Promise<void>;

  /* action creators */
  /**
   * A function for adding a lao server address
   * @param laoId The lao id
   * @param address The address that should be added
   * @returns A redux action
   */
  addLaoServerAddress: (laoId: Hash, address: string) => AnyAction;

  /* hooks */

  /**
   * Gets the current lao list
   * @returns The current lao list
   */
  useLaoList: () => HomeFeature.Lao[];

  /**
   * Gets the function to disconnect from the current lao
   */
  useDisconnectFromLao: () => () => void;

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
  | 'resubscribeToLao'
  | 'useCurrentLaoId'
  | 'useDisconnectFromLao'
  | 'getLaoById'
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
    encodeLaoConnectionForQRCode: (servers: string[], laoId: Hash) => string;
  };
  context: HomeReactContext;
}
