import { AnyAction, Reducer } from 'redux';
import { KeyPairRegistry } from 'core/keypair';
import { MessageRegistry } from 'core/network/jsonrpc/messages';
import { Hash, PublicKey } from 'core/objects';
import FeatureInterface from 'core/objects/FeatureInterface';

import { LinkedOrganizationsFeature } from './Feature';
import { CHALLENGE_REDUCER_PATH, ChallengeReducerState } from '../reducer';
import {
  LinkedOrganizationReducerState,
  LINKEDORGANIZATIONS_REDUCER_PATH,
} from '../reducer/LinkedOrganizationsReducer';
import { Lao } from 'features/lao/objects';

export const LINKED_ORGANIZATIONS_FEATURE_IDENTIFIER = 'linked-organizations';

export interface LinkedOrganizationsCompositionConfiguration {
  /* objects */

  keyPairRegistry: KeyPairRegistry;
  messageRegistry: MessageRegistry;

  /* lao */

  /**
   * Returns the currently active lao id. Should be used outside react components
   * @returns The current lao or undefined if there is none.
   */
  getCurrentLaoId: () => Hash | undefined;

  /**
   * Returns the currently active lao. Should be used outside react components
   * @returns The current lao
   */
  useCurrentLaoId: () => Hash;

  /**
   * Gets whether the current user is organizer of the given lao
   * To use only in a React component
   */
  useIsLaoOrganizer: (laoId: Hash) => boolean;

  /**
   * Given a lao id, this function returns the public key of the backend
   * @param laoId The id of the lao
   * @returns The public key or undefined if none is known
   */
  getLaoOrganizerBackendPublicKey: (laoId: Hash) => PublicKey | undefined;

  /**
 * Gets the current lao
 * @returns The current lao
 */
  useCurrentLao: () => Lao;
}

/**
 * The type of the context that is provided to react linked organizations components
 */
export type LinkedOrganizationsReactContext = Pick<
  LinkedOrganizationsCompositionConfiguration,
  'useCurrentLaoId' | 'useIsLaoOrganizer' | 'useCurrentLao'
>;

/**
 * The interface the linked organizations feature exposes
 */
export interface LinkedOrganizationsInterface extends FeatureInterface {
  laoScreens: LinkedOrganizationsFeature.LaoScreen[];
}

export interface LinkedOrganizationsCompositionInterface extends FeatureInterface {
  context: LinkedOrganizationsReactContext;
  reducers: {
    [CHALLENGE_REDUCER_PATH]: Reducer<ChallengeReducerState>;
    [LINKEDORGANIZATIONS_REDUCER_PATH]: Reducer<LinkedOrganizationReducerState>;
  };
}
