import { SOCIAL_FEATURE_IDENTIFIER, SocialConfiguration } from './interface';
import * as navigation from './navigation';
import { configureNetwork } from './network';
import { socialReducer } from './reducer';
import {
  SocialFollowsScreen,
  SocialHomeScreen,
  SocialProfileScreen,
  SocialSearchScreen,
  SocialUserProfileScreen,
} from './screens';

/**
 * Configures the social media feature
 *
 * @param configuration - The configuration object for the social media feature
 */
export function configure(configuration: SocialConfiguration) {
  const {
    useCurrentLao,
    getCurrentLao,
    useCurrentLaoId,
    getCurrentLaoId,
    useRollCallById,
    useRollCallAttendeesById,
    generateToken,
  } = configuration;
  configureNetwork(configuration);
  return {
    identifier: SOCIAL_FEATURE_IDENTIFIER,
    navigation,
    reducers: {
      ...socialReducer,
    },
    context: {
      /* lao */
      useCurrentLao,
      getCurrentLao,
      useCurrentLaoId,
      getCurrentLaoId,
      /* roll call */
      useRollCallById,
      useRollCallAttendeesById,
      /* wallet */
      generateToken,
    },
    socialScreens: [SocialHomeScreen, SocialSearchScreen, SocialFollowsScreen, SocialProfileScreen],
    socialSearchScreens: [SocialSearchScreen, SocialUserProfileScreen],
  };
}
