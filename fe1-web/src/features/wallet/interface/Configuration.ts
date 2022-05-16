import React from 'react';
import { Reducer } from 'redux';

import { KeyPairRegistry } from 'core/keypair';
import { Hash } from 'core/objects';
import FeatureInterface from 'core/objects/FeatureInterface';

import { WalletReducerState, WALLET_REDUCER_PATH } from '../reducer';
import { WalletFeature } from './Feature';

export const WALLET_FEATURE_IDENTIFIER = 'wallet';

export interface WalletConfiguration {
  // objects
  keyPairRegistry: KeyPairRegistry;

  /* LAO related functions */

  /**
   * Returns the currently active lao. Should be used outside react components
   * @returns The current lao
   */
  getCurrentLao: () => WalletFeature.Lao;

  /**
   * Returns the currently active lao id. Should be used inside react components
   * @returns The current lao id
   */
  useCurrentLaoId: () => Hash | undefined;

  /* Event related functions */

  /**
   * Given the redux state and an event id, this function looks in the active
   * lao for an event with a matching id, creates an instance of the corresponding type
   * and returns it
   * @param id - The id of the event
   * @returns The event or undefined if none was found
   */
  getEventById: (id: Hash) => WalletFeature.Event | undefined;

  /**
   * Creates a two-level map from laoIds to eventIds to events
   * where all returned events have type 'eventType'
   * @param eventType The type of the events that should be returned
   * @returns A map from laoIds to a map of eventIds to events
   */
  makeEventByTypeSelector: (
    eventType: string,
  ) => (state: unknown) => Record<string, Record<string, WalletFeature.Event>>;
}

/**
 * The type of the context that is provided to react wallet components
 */
export type WalletReactContext = Pick<
  WalletConfiguration,
  /* lao */
  | 'useCurrentLaoId'
  /* events */
  | 'makeEventByTypeSelector'
>;

/**
 * The interface the wallet feature exposes
 */
export interface WalletInterface extends FeatureInterface {
  navigation: {
    WalletNavigation: React.ComponentType<any>;
  };

  context: WalletReactContext;

  reducers: {
    [WALLET_REDUCER_PATH]: Reducer<WalletReducerState>;
  };
}
