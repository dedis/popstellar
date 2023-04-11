import { PoPchaConfiguration, PoPchaInterface } from './interface';
import { popchaScannerScreen } from './screens/PoPchaScanner';

export const configure = (configuration: PoPchaConfiguration): PoPchaInterface => {
  console.log('PoPcha configuration: ', configuration);
  return {
    laoScreens: [popchaScannerScreen],
  };
};
