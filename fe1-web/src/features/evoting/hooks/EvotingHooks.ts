import { useContext } from 'react';

import FeatureContext from 'core/contexts/FeatureContext';

import { EvotingReactContext, EVOTING_FEATURE_IDENTIFIER } from '../interface';

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
    const laoId = useEvotingContext().useCurrentLaoId();

    if (!laoId) {
      throw new Error('Error encountered while obtaining current lao id: no active LAO');
    }

    return laoId;
  };

  /**
   * Gets the current lao
   * @returns The current lao
   */
  export const useCurrentLao = () => {
    return useEvotingContext().useCurrentLao();
  };

  /**
   * Gets the organizer backend's public key for a given lao id
   * @param laoId The lao id for which the key should be retrieved
   */
  export const useLaoOrganizerBackendPublicKey = (laoId: string) => {
    const key = useEvotingContext().useLaoOrganizerBackendPublicKey(laoId);

    if (!key) {
      throw new Error("Could not obtain the public key of the lao organizer's backend");
    }

    return key;
  };
}
