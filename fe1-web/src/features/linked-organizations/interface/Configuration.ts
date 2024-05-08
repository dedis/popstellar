import { KeyPairRegistry } from 'core/keypair';
import { MessageRegistry } from 'core/network/jsonrpc/messages';
import { Hash } from 'core/objects';
import FeatureInterface from 'core/objects/FeatureInterface';

import { LinkedOrganizationsFeature } from './Feature';

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
  useCurrentLaoId: () => Hash;

  /**
   * Gets whether the current user is organizer of the given lao
   * To use only in a React component
   */
  useIsLaoOrganizer: (laoId: Hash) => boolean;
}

/**
 * The type of the context that is provided to react linked organizations components
 */
export type LinkedOrganizationsReactContext = Pick<
  LinkedOrganizationsCompositionConfiguration,
  'useCurrentLaoId' | 'useIsLaoOrganizer'
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
