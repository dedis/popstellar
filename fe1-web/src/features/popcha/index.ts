import { POPCHA_FEATURE_IDENTIFIER, PopchaConfiguration, PopchaInterface } from './interface';
import { configureNetwork } from './network';
import { popchaScannerScreen } from './screens/PopchaScanner';

export const configure = (configuration: PopchaConfiguration): PopchaInterface => {
  configureNetwork(configuration);

  return {
    identifier: POPCHA_FEATURE_IDENTIFIER,
    context: {
      useCurrentLaoId: configuration.useCurrentLaoId,
      generateToken: configuration.generateToken,
    },
    laoScreens: [popchaScannerScreen],
  };
};
