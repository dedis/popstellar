import { useContext } from 'react';

import FeatureContext from 'core/contexts/FeatureContext';

import { WintessReactContext, WITNESS_FEATURE_IDENTIFIER } from '../interface/Configuration';

export namespace WitnessHooks {
  /* Hooks passed by dependencies */

  export const useNotificationContext = (): WintessReactContext => {
    const featureContext = useContext(FeatureContext);
    // assert that the witness context exists
    if (!(WITNESS_FEATURE_IDENTIFIER in featureContext)) {
      throw new Error('Witness context could not be found!');
    }
    return featureContext[WITNESS_FEATURE_IDENTIFIER] as WintessReactContext;
  };

  /**
   * Gets discard notifications action creator
   * @returns The action creator
   */
  export const useDiscardNotifications = () => useNotificationContext().discardNotifications;

  /**
   * Gets mark notification as read action creator
   * @returns The action creator
   */
  export const useMarkNotificationAsRead = () => useNotificationContext().markNotificationAsRead;

  /**
   * Checks if the witness feature is enabled
   * @returns Whether the feature is enabled or not
   */
  export const useIsEnabled = () => useNotificationContext().enabled;
}
