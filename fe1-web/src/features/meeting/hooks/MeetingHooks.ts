import { useContext } from 'react';

import FeatureContext from 'core/contexts/FeatureContext';

import { MeetingReactContext, MEETING_FEATURE_IDENTIFIER } from '../interface';

export namespace MeetingHooks {
  export const useMeetingContext = (): MeetingReactContext => {
    const featureContext = useContext(FeatureContext);
    // assert that the meeting context exists
    if (!(MEETING_FEATURE_IDENTIFIER in featureContext)) {
      throw new Error('Meeting context could not be found!');
    }
    return featureContext[MEETING_FEATURE_IDENTIFIER] as MeetingReactContext;
  };

  /**
   * Gets the current lao id
   * @returns The current lao id
   */
  export const useCurrentLaoId = () => {
    const laoId = useMeetingContext().useCurrentLaoId();

    if (!laoId) {
      throw new Error('Error encountered while obtaining current lao id: no active LAO');
    }

    return laoId;
  };
}
