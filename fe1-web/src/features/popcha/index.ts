import { POPCHA_FEATURE_IDENTIFIER, PoPchaConfiguration, PoPchaInterface } from './interface';
import { popchaScannerScreen } from './screens/PoPchaScanner';

export const configure = (configuration: PoPchaConfiguration): PoPchaInterface => {
  console.log('PoPcha configuration: ', configuration);
  return {
    identifier: POPCHA_FEATURE_IDENTIFIER,
    context: { useCurrentLaoId: configuration.useCurrentLaoId },
    laoScreens: [popchaScannerScreen],
  };
};
