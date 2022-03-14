import FeatureContext from 'core/contexts/FeatureContext';
import { useContext } from 'react';
import { EventsReactContext, EVENTS_FEATURE_IDENTIFIER } from '../interface';

export namespace EventsHooks {
  export const useEventsContext = (): EventsReactContext => {
    const featureContext = useContext(FeatureContext);
    // assert that the evoting context exists
    if (!(EVENTS_FEATURE_IDENTIFIER in featureContext)) {
      throw new Error('Events context could not be found!');
    }
    return featureContext[EVENTS_FEATURE_IDENTIFIER] as EventsReactContext;
  };

  /**
   * Gets whether the current user is organizer of the current lao
   * @returns Whether the current user is organizer of the current lao
   */
  export const useIsLaoOrganizer = () => {
    return useEventsContext().useIsLaoOrganizer();
  };
}
