
import FeatureInterface from 'core/objects/FeatureInterface';
import { KeyPairRegistry } from 'core/keypair';
import { MessageRegistry } from 'core/network/jsonrpc/messages';

import { LinkedOrganizationsFeature } from './Feature';
import { Hash, PublicKey } from 'core/objects';

export const LINKED_ORGANIZATIONS_FEATURE_IDENTIFIER = 'linked-organizations';

export interface LinkedOrganizationsCompositionConfiguration {
  /* objects */

  keyPairRegistry: KeyPairRegistry;
  messageRegistry: MessageRegistry;



  /* lao */

  /**
   * Returns the currently active lao. Should be used outside react components
   * @returns The current lao
   */
  getCurrentLao: () => LinkedOrganizationsFeature.Lao;

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
  useIsLaoOrganizer: (laoId: Hash) => boolean;

  /**
   * Gets the organizer's public key of the given lao
   * @param laoId
   * @returns the organizer's public key or undefined if lao not found
   */
  getLaoOrganizer: (laoId: Hash) => PublicKey | undefined;
}


  /**
   * The type of the context that is provided to react digital cash components
   */
export type LinkedOrganizationsReactContext = Pick<
LinkedOrganizationsCompositionConfiguration,
/* lao */
| 'useCurrentLaoId'
| 'useIsLaoOrganizer'
>;



/**
 * The interface the linked organizations feature exposes
 */
export interface LinkedOrganizationsInterface extends FeatureInterface {
  laoScreens: LinkedOrganizationsFeature.LaoScreen[];
  
}

export interface LinkedOrganizationsCompositionInterface extends FeatureInterface {
  context: LinkedOrganizationsReactContext;
  reducers: {};
}
