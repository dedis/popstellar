import { HomeCompositionConfiguration, HomeInterface, HOME_FEATURE_IDENTIFIER } from './interface';
import * as navigation from './navigation';
import * as screens from './screens';

/**
 * Configures the Home feature
 */
export function compose(config: HomeCompositionConfiguration): HomeInterface {
  return {
    identifier: HOME_FEATURE_IDENTIFIER,
    navigation,
    screens,
    context: {
      requestCreateLao: config.requestCreateLao,
      addLaoServerAddress: config.addLaoServerAddress,
      connectToTestLao: config.connectToTestLao,
      useLaoList: config.useLaoList,
      LaoList: config.LaoList,
      mainNavigationScreens: config.mainNavigationScreens,
    },
  };
}
