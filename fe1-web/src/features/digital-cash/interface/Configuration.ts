import { Reducer } from 'redux';

import { KeyPairRegistry } from 'core/keypair';
import { MessageRegistry } from 'core/network/jsonrpc/messages';
import { Hash, PublicKey } from 'core/objects';
import FeatureInterface from 'core/objects/FeatureInterface';
import { RollCallToken } from 'core/objects/RollCallToken';

import { DIGITAL_CASH_REDUCER_PATH, DigitalCashLaoReducerState } from '../reducer';
import { DigitalCashFeature } from './Feature';

export const DIGITAL_CASH_FEATURE_IDENTIFIER = 'digital-cash';

export interface DigitalCashCompositionConfiguration {
  /* objects */

  keyPairRegistry: KeyPairRegistry;
  messageRegistry: MessageRegistry;

  /* lao */

  /**
   * Returns the currently active lao. Should be used outside react components
   * @returns The current lao
   */
  getCurrentLao: () => DigitalCashFeature.Lao;

  /**
   * Returns the currently active lao id. Should be used outside react components
   * @returns The current lao or undefined if there is none.
   */
  getCurrentLaoId: () => Hash | undefined;

  /**
   * Returns the currently active lao id. Should be used inside react components.
   * Throws an error if there is no currently active lao
   */
  useCurrentLaoId: () => Hash;

  /**
   * Returns true if currently connected to a lao, false if in offline mode
   * and undefined if there is no current lao
   */
  useConnectedToLao: () => boolean | undefined;

  /**
   * Gets whether the current user is organizer of the given lao
   * To use only in a React component
   */
  useIsLaoOrganizer: (laoId: Hash | string) => boolean;

  /**
   * Gets the organizer's public key of the given lao
   * @param laoId
   * @returns the organizer's public key or undefined if lao not found
   */
  getLaoOrganizer: (laoId: Hash | string) => PublicKey | undefined;

  /* Roll Calls */

  /**
   * Gets the roll call associated to this roll call id
   * To use only in a React component
   * @param rollCallId
   * @returns The roll call or undefined if not found
   */
  useRollCallById: (rollCallId: Hash | string) => DigitalCashFeature.RollCall | undefined;

  /**
   * Gets all roll calls associated to this lao id
   * To use only in a React component
   * @param laoId
   */
  useRollCallsByLaoId: (laoId: Hash | string) => {
    [rollCallId: string]: DigitalCashFeature.RollCall;
  };

  /* Roll Call Tokens */

  /**
   * Gets all roll call tokens associated to this lao for the current user and its seed
   * To use only in a React component
   * @param laoId
   */
  useRollCallTokensByLaoId: (laoId: Hash | string) => RollCallToken[];

  /**
   * Gets the roll call token associated to this lao and this roll call for the current user and its seed
   * @param laoId
   * @param rollCallId
   * @returns the RollCallToken or undefined if not found in the roll call
   */
  useRollCallTokenByRollCallId: (
    laoId: Hash | string,
    rollCallId: string,
  ) => RollCallToken | undefined;
}

/**
 * The type of the context that is provided to react digital cash components
 */
export type DigitalCashReactContext = Pick<
  DigitalCashCompositionConfiguration,
  /* lao */
  | 'useCurrentLaoId'
  | 'useIsLaoOrganizer'
  | 'useConnectedToLao'

  /* roll call */
  | 'useRollCallById'
  | 'useRollCallsByLaoId'

  /* roll call tokens */
  | 'useRollCallTokensByLaoId'
  | 'useRollCallTokenByRollCallId'
>;

/**
 * The interface the digital cash feature exposes
 */
export interface DigitalCashInterface extends FeatureInterface {
  walletItemGenerators: DigitalCashFeature.WalletItemGenerator[];
  walletScreens: DigitalCashFeature.WalletScreen[];
}

export interface DigitalCashCompositionInterface extends FeatureInterface {
  context: DigitalCashReactContext;
  reducers: {
    [DIGITAL_CASH_REDUCER_PATH]: Reducer<DigitalCashLaoReducerState>;
  };
}
