import { useContext } from 'react';

import FeatureContext from 'core/contexts/FeatureContext';

import { WitnessReactContext, WITNESS_FEATURE_IDENTIFIER } from '../interface/Configuration';

export namespace WitnessHooks {
  /* Hooks passed by dependencies */

  export const useWitnessContext = (): WitnessReactContext => {
    const featureContext = useContext(FeatureContext);
    // assert that the witness context exists
    if (!(WITNESS_FEATURE_IDENTIFIER in featureContext)) {
      throw new Error('Witness context could not be found!');
    }
    return featureContext[WITNESS_FEATURE_IDENTIFIER] as WitnessReactContext;
  };

  /**
   * A hook returning the current lao id
   * @returns The current lao id
   */
  export const useCurrentLaoId = () => {
    const laoId = useWitnessContext().useCurrentLaoId();

    if (!laoId) {
      throw new Error('You are currently not connected to a lao!');
    }
    return laoId;
  };

  /**
   * Gets discard notifications action creator
   * @returns The action creator
   */
  export const useDiscardNotifications = () => useWitnessContext().discardNotifications;

  /**
   * Gets mark notification as read action creator
   * @returns The action creator
   */
  export const useMarkNotificationAsRead = () => useWitnessContext().markNotificationAsRead;

  /**
   * Checks if the witness feature is enabled
   * @returns Whether the feature is enabled or not
   */
  export const useIsEnabled = () => useWitnessContext().enabled;
}
