import * as functions from './functions';
import { HomeCompositionConfiguration, HomeInterface, HOME_FEATURE_IDENTIFIER } from './interface';
import { ConnectNavigationScreen } from './navigation/ConnectNavigation';
import { MainNavigationScreen } from './navigation/MainNavigation';
import * as screens from './screens';

/**
 * Configures the Home feature
 */
export function compose(config: HomeCompositionConfiguration): HomeInterface {
  return {
    identifier: HOME_FEATURE_IDENTIFIER,
    appScreens: [MainNavigationScreen, ConnectNavigationScreen],
    screens,
    functions,
    context: {
      requestCreateLao: config.requestCreateLao,
      addLaoServerAddress: config.addLaoServerAddress,
      connectToTestLao: config.connectToTestLao,
      useLaoList: config.useLaoList,
      LaoList: config.LaoList,
      mainNavigationScreens: config.mainNavigationScreens,
      getLaoChannel: config.getLaoChannel,
      useCurrentLaoId: config.useCurrentLaoId,
    },
  };
}
