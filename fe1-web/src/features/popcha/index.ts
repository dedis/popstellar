import { POPCHA_FEATURE_IDENTIFIER, PoPchaConfiguration, PoPchaInterface } from './interface';
import { popchaScannerScreen } from './screens/PoPchaScanner';

export const configure = (configuration: PoPchaConfiguration): PoPchaInterface => {
  return {
    identifier: POPCHA_FEATURE_IDENTIFIER,
    context: {
      useCurrentLaoId: configuration.useCurrentLaoId,
      generateToken: configuration.generateToken,
    },
    laoScreens: [popchaScannerScreen],
  };
};
