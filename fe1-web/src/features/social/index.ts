import { SOCIAL_FEATURE_IDENTIFIER, SocialConfiguration, SocialInterface } from './interface';
import { SocialMediaScreen } from './navigation/SocialMediaNavigation';
import { configureNetwork } from './network';
import { socialReducer } from './reducer';

/**
 * Configures the social media feature
 *
 * @param configuration - The configuration object for the social media feature
 */
export function configure(configuration: SocialConfiguration): SocialInterface {
  const {
    useCurrentLao,
    getCurrentLao,
    useCurrentLaoId,
    getCurrentLaoId,
    useConnectedToLao,
    useRollCallById,
    useRollCallAttendeesById,
    generateToken,
  } = configuration;

  configureNetwork(configuration);

  return {
    identifier: SOCIAL_FEATURE_IDENTIFIER,
    reducers: {
      ...socialReducer,
    },
    context: {
      /* lao */
      useCurrentLao,
      getCurrentLao,
      useCurrentLaoId,
      getCurrentLaoId,
      useConnectedToLao,
      /* roll call */
      useRollCallById,
      useRollCallAttendeesById,
      /* wallet */
      generateToken,
    },
    laoScreens: [SocialMediaScreen],
  };
}
