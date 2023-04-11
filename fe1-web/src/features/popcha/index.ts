import { PoPchaConfiguration, PoPchaInterface } from './interface';
import { simpleScreenScreen } from './screens/simpleScreen';

export const configure = (configuration: PoPchaConfiguration): PoPchaInterface => {
  console.log('PoPcha configuration: ', configuration);
  return {
    laoScreens: [simpleScreenScreen],
  };
};
