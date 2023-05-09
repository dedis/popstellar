import { POPCHA_FEATURE_IDENTIFIER, PopchaConfiguration, PopchaInterface } from './interface';
import { popchaScannerScreen } from './screens/PopchaScanner';

export const configure = (configuration: PopchaConfiguration): PopchaInterface => {
  return {
    identifier: POPCHA_FEATURE_IDENTIFIER,
    context: {
      useCurrentLaoId: configuration.useCurrentLaoId,
      generateToken: configuration.generateToken,
    },
    laoScreens: [popchaScannerScreen],
  };
};
