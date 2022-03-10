import { useContext } from 'react';

import FeatureContext from 'core/contexts/FeatureContext';

import { EVOTING_FEATURE_IDENTIFIER } from '../index';
import { EvotingReactContext } from '../objects';

export namespace EvotingHooks {
  export const useEvotingContext = (): EvotingReactContext => {
    const featureContext = useContext(FeatureContext);
    // assert that the evoting context exists
    if (!(EVOTING_FEATURE_IDENTIFIER in featureContext)) {
      throw new Error('Evoting context could not be found!');
    }
    return featureContext[EVOTING_FEATURE_IDENTIFIER] as EvotingReactContext;
  };
  /**
   * Gets the current lao id
   * @returns The current lao id
   */
  export const useCurrentLaoId = () => {
    return useEvotingContext().getCurrentLaoId();
  };

  /**
   * Gets the current lao
   * @returns The current lao
   */
  export const useCurrentLao = () => {
    return useEvotingContext().getCurrentLao();
  };

  /**
   * Gets the onConfirmEventCreation helper function
   * @returns The onConfirmEventCreation function
   */
  export const useOnConfirmEventCreation = () => {
    return useEvotingContext().onConfirmEventCreation;
  };
}
