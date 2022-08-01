import { SOCIAL_FEATURE_IDENTIFIER, SocialConfiguration } from './interface';
import * as navigation from './navigation';
import { configureNetwork } from './network';
import { socialReducer } from './reducer';

/**
 * Configures the social media feature
 *
 * @param configuration - The configuration object for the social media feature
 */
export function configure(configuration: SocialConfiguration) {
  configureNetwork(configuration);
  return {
    identifier: SOCIAL_FEATURE_IDENTIFIER,
    navigation,
    reducers: {
      ...socialReducer,
    },
  };
}
