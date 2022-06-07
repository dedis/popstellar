import React from 'react';
import { Reducer } from 'redux';

import { KeyPairRegistry } from 'core/keypair';
import { MessageRegistry } from 'core/network/jsonrpc/messages';
import { Hash, PopToken, PublicKey } from 'core/objects';
import FeatureInterface from 'core/objects/FeatureInterface';

import {
  WalletReducerState,
  WALLET_REDUCER_PATH,
  DIGITAL_CASH_REDUCER_PATH,
  DigitalCashLaoReducerState,
} from '../reducer';
import { WalletFeature } from './Feature';

export const WALLET_FEATURE_IDENTIFIER = 'wallet';

export interface WalletConfiguration {}

export interface WalletInterface extends FeatureInterface {
  functions: {
    /**
     * Deterministically generates a pop token from given lao and rollCall ids
     * @param laoId The lao id to generate a token for
     * @param rollCallId The rollCall id to generate a token for
     * @returns The generated pop token
     */
    generateToken: (laoId: Hash, rollCallId: Hash | undefined) => Promise<PopToken>;

    /**
     * Returns whether a seed is present in the store
     */
    hasSeed: () => boolean;
  };
}

export interface WalletCompositionConfiguration {
  // objects
  keyPairRegistry: KeyPairRegistry;
  messageRegistry: MessageRegistry;

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

  /**
   * Returns the public key of the organizer of the lao
   * @param laoId the id of the lao
   */
  getLaoOrganizer: (laoId: string) => PublicKey | undefined;

  /* Event related functions */

  /**
   * Given the redux state and an event id, this function looks in the active
   * lao for an event with a matching id, creates an instance of the corresponding type
   * and returns it
   * @param id - The id of the event
   * @returns The event or undefined if none was found
   */
  getEventById: (id: Hash) => WalletFeature.EventState | undefined;

  /**
   * Returns a two-level map from laoIds to rollCallIds to rollCalls
   */
  useRollCallsByLaoId: () => {
    [laoId: string]: { [rollCallId: string]: WalletFeature.RollCall };
  };

  getRollCallById: (id: Hash) => WalletFeature.RollCall | undefined;
}

/**
 * The type of the context that is provided to react wallet components
 */
export type WalletReactContext = Pick<
  WalletCompositionConfiguration,
  /* lao */
  | 'useCurrentLaoId'
  | 'getLaoOrganizer'
  /* events */
  | 'useRollCallsByLaoId'
>;

/**
 * The interface the wallet feature exposes
 */
export interface WalletCompositionInterface extends FeatureInterface {
  navigation: {
    WalletNavigation: React.ComponentType<any>;
  };

  context: WalletReactContext;

  reducers: {
    [WALLET_REDUCER_PATH]: Reducer<WalletReducerState>;
    [DIGITAL_CASH_REDUCER_PATH]: Reducer<DigitalCashLaoReducerState>;
  };
}
