import { useContext } from 'react';

import FeatureContext from 'core/contexts/FeatureContext';
import { Hash } from 'core/objects';

import { SOCIAL_FEATURE_IDENTIFIER, SocialReactContext } from '../interface';

export namespace SocialHooks {
  export const useSocialContext = (): SocialReactContext => {
    const featureContext = useContext(FeatureContext);
    // assert that the social context exists
    if (!(SOCIAL_FEATURE_IDENTIFIER in featureContext)) {
      throw new Error('Social context could not be found!');
    }
    return featureContext[SOCIAL_FEATURE_IDENTIFIER] as SocialReactContext;
  };

  /**
   * Gets the current lao
   * @returns The current lao
   */
  export const useCurrentLao = () => {
    return useSocialContext().useCurrentLao();
  };

  /**
   * Gets the current lao id
   */
  export const useCurrentLaoId = () => useSocialContext().useCurrentLaoId();

  /**
   * Gets the roll call for a given id
   */
  export const useRollCallById = (rollCallId: Hash | string) =>
    useSocialContext().useRollCallById(rollCallId);

  /**
   * Gets the generateToken function
   */
  export const useGenerateToken = () => useSocialContext().generateToken;

  /**
   * Gets the attendees list for a roll call given its id.
   * @param rollCallId
   */
  export const useRollCallAttendeesList = (rollCallId: Hash | string) =>
    useSocialContext().useRollCallAttendeesList(rollCallId);
}
