import * as functions from './functions';
import { HomeCompositionConfiguration, HomeInterface, HOME_FEATURE_IDENTIFIER } from './interface';
import { ConnectNavigationScreen } from './navigation/ConnectNavigation';
import { HomeNavigationScreen } from './navigation/HomeNavigation';
import * as screens from './screens';

/**
 * Configures the Home feature
 */
export function compose(config: HomeCompositionConfiguration): HomeInterface {
  return {
    identifier: HOME_FEATURE_IDENTIFIER,
    appScreens: [HomeNavigationScreen, ConnectNavigationScreen],
    screens,
    functions,
    context: {
      requestCreateLao: config.requestCreateLao,
      addLaoServerAddress: config.addLaoServerAddress,
      connectToTestLao: config.connectToTestLao,
      useLaoList: config.useLaoList,
      LaoList: config.LaoList,
      homeNavigationScreens: config.homeNavigationScreens,
      getLaoChannel: config.getLaoChannel,
      useCurrentLaoId: config.useCurrentLaoId,
    },
  };
}
