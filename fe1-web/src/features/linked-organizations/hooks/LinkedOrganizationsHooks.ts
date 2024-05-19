import { useContext } from 'react';

import FeatureContext from 'core/contexts/FeatureContext';
import { Hash } from 'core/objects';

import {
  LINKED_ORGANIZATIONS_FEATURE_IDENTIFIER,
  LinkedOrganizationsReactContext,
} from '../interface';

export namespace LinkedOrganizationsHooks {
  export const useLinkedOrganizationsContext = (): LinkedOrganizationsReactContext => {
    const featureContext = useContext(FeatureContext);
    // assert that the linked organizations context exists
    if (!(LINKED_ORGANIZATIONS_FEATURE_IDENTIFIER in featureContext)) {
      throw new Error('Linked organization context could not be found!');
    }
    return featureContext[
      LINKED_ORGANIZATIONS_FEATURE_IDENTIFIER
    ] as LinkedOrganizationsReactContext;
  };

  /**
   * Gets the current lao id, throws an error if there is none
   */
  export const useCurrentLaoId = () => useLinkedOrganizationsContext().useCurrentLaoId();

    /**
   * Gets the current lao , throws an error if there is none
   */
    export const useCurrentLao = () => useLinkedOrganizationsContext().useCurrentLao();

  /**
   * Gets whether the current user is organizer of the given lao
   */
  export const useIsLaoOrganizer = (laoId: Hash) =>
    useLinkedOrganizationsContext().useIsLaoOrganizer(laoId);

  
}
