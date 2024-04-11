
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
   * A hook returning the current lao id
   * @returns The current lao id
   */
  useCurrentLaoId: () => Hash;

  /**
   * Gets whether the current user is organizer of the given lao
   * To use only in a React component
   */
  useIsLaoOrganizer: (laoId: Hash) => boolean;


}


  /**
   * The type of the context that is provided to react digital cash components
   */
export type LinkedOrganizationsReactContext = Pick<
LinkedOrganizationsCompositionConfiguration,'useCurrentLaoId' | 'useIsLaoOrganizer'>;



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
