import { useContext } from 'react';

import FeatureContext from 'core/contexts/FeatureContext';

import { WitnessReactContext, WITNESS_FEATURE_IDENTIFIER } from '../interface';

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
   * A hook returning the current lao id or throws an error of there is none
   */
  export const useAssertCurrentLaoId = () => useWitnessContext().useAssertCurrentLaoId();

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
