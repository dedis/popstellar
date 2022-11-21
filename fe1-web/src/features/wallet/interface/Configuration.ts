import { Reducer } from 'redux';

import { KeyPairRegistry } from 'core/keypair';
import { AppScreen } from 'core/navigation/AppNavigation';
import { MessageRegistry } from 'core/network/jsonrpc/messages';
import { Hash, PopToken } from 'core/objects';
import FeatureInterface from 'core/objects/FeatureInterface';
import { RollCallToken } from 'core/objects/RollCallToken';

import { WalletReducerState, WALLET_REDUCER_PATH } from '../reducer';
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

    /**
     * Forgets the current seed
     */
    forgetSeed: () => void;
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
   * Returns the currently active lao id, throws error if there is none.
   * Should be used inside react components
   */
  useCurrentLaoId: () => Hash;

  /**
   * Returns the currently active lao, throws error if there is none.
   * Should be used inside react components
   */
  useCurrentLao: () => WalletFeature.Lao;

  /**
   * Returns true if currently connected to a lao, false if in offline mode
   * and undefined if there is no current lao
   */
  useConnectedToLao: () => boolean | undefined;

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
   * Returns a map from rollCallIds to rollCalls for a given lao id
   */
  useRollCallsByLaoId: (laoId: string) => {
    [rollCallId: string]: WalletFeature.RollCall;
  };

  /**
   * Returns a map from laoIds to names
   */

  getRollCallById: (id: Hash) => WalletFeature.RollCall | undefined;

  useRollCallTokensByLaoId: (laoId: string) => RollCallToken[];

  /**
   * A list of item generators that given a laoId return a list of items
   * to be displayed in the wallet for a given lao
   */
  walletItemGenerators: WalletFeature.WalletItemGenerator[];

  walletNavigationScreens: WalletFeature.WalletScreen[];
}

/**
 * The type of the context that is provided to react wallet components
 */
export type WalletReactContext = Pick<
  WalletCompositionConfiguration,
  /* parameters */
  | 'walletItemGenerators'
  | 'walletNavigationScreens'
  /* lao */
  | 'useCurrentLaoId'
  | 'useCurrentLao'
  | 'useConnectedToLao'
  /* events */
  | 'useRollCallsByLaoId'
  | 'useRollCallTokensByLaoId'
>;

/**
 * The interface the wallet feature exposes
 */
export interface WalletCompositionInterface extends FeatureInterface {
  appScreens: AppScreen[];

  laoScreens: WalletFeature.LaoScreen[];

  context: WalletReactContext;

  reducers: {
    [WALLET_REDUCER_PATH]: Reducer<WalletReducerState>;
  };
}
