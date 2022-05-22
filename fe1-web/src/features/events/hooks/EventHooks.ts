import { useContext } from 'react';

import FeatureContext from 'core/contexts/FeatureContext';

import { EventReactContext, EVENT_FEATURE_IDENTIFIER } from '../interface';

export namespace EventHooks {
  export const useEventsContext = (): EventReactContext => {
    const featureContext = useContext(FeatureContext);
    // assert that the evoting context exists
    if (!(EVENT_FEATURE_IDENTIFIER in featureContext)) {
      throw new Error('Events context could not be found!');
    }
    return featureContext[EVENT_FEATURE_IDENTIFIER] as EventReactContext;
  };

  /**
   * Gets the current lao id
   */
  export const useCurrentLaoId = () => useEventsContext().useCurrentLaoId();

  /**
   * Gets whether the current user is organizer of the current lao
   * @returns Whether the current user is organizer of the current lao
   */
  export const useIsLaoOrganizer = () => useEventsContext().useIsLaoOrganizer();

  /**
   * Gets the list of registrered event type components
   * @returns The list of registered event type components
   */
  export const useEventTypes = () => useEventsContext().eventTypes;
}
