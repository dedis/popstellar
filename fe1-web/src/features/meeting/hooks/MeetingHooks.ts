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
   * Returns the currently active lao id or throws an error if there is none.
   * Should be used inside react components
   */
  export const useCurrentLaoId = () => useMeetingContext().useCurrentLaoId();

  /**
   * Returns true if currently connected to a lao, false if in offline mode
   * and undefined if there is no current lao
   */
  export const useConnectedToLao = () => useMeetingContext().useConnectedToLao();
}
