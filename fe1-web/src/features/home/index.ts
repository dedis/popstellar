import * as functions from './functions';
import { HomeCompositionConfiguration, HomeInterface, HOME_FEATURE_IDENTIFIER } from './interface';
import { HomeNavigationScreen } from './navigation/HomeNavigation';

/**
 * Configures the Home feature
 */
export function compose(config: HomeCompositionConfiguration): HomeInterface {
  return {
    identifier: HOME_FEATURE_IDENTIFIER,
    appScreens: [HomeNavigationScreen],
    functions,
    context: {
      requestCreateLao: config.requestCreateLao,
      addLaoServerAddress: config.addLaoServerAddress,
      connectToTestLao: config.connectToTestLao,
      useLaoList: config.useLaoList,
      LaoList: config.LaoList,
      homeNavigationScreens: config.homeNavigationScreens,
      getLaoChannel: config.getLaoChannel,
      useConnectedToLao: config.useConnectedToLao,
      useDisconnectFromLao: config.useDisconnectFromLao,
      getLaoById: config.getLaoById,
      resubscribeToLao: config.resubscribeToLao,
    },
  };
}
