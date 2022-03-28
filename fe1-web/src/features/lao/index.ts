import { catchup, getNetworkManager, subscribeToChannel } from 'core/network';
import { getStore } from 'core/redux';

import { PublicComponents } from './components';
import * as functions from './functions';
import * as hooks from './hooks';
import {
  LaoCompositionConfiguration,
  LaoCompositionInterface,
  LaoConfiguration,
  LaoConfigurationInterface,
  LAO_FEATURE_IDENTIFIER,
} from './interface';
import * as navigation from './navigation';
import { configureNetwork } from './network';
import { selectCurrentLaoId, laoReducer, addLaoServerAddress, addNotification, discardNotification, markNotificationAsRead, notificationReducer } from './reducer';

/**
 * Configures the LAO feature
 *
 * @param config - The configuration object
 */

export const configure = (config: LaoConfiguration): LaoConfigurationInterface => {
  configureNetwork(config.registry);

  // in case of a reconnection, send a catchup message on the root channel
  getNetworkManager().addReconnectionHandler(async () => {
    // after reconnecting, check whether we have already been connected to a LAO

    const laoId = selectCurrentLaoId(getStore().getState());
    if (!laoId) {
      return;
    }

    // if yes - then subscribe to the LAO channel and send a catchup
    const channel = functions.getLaoChannel(laoId.valueOf());

    await subscribeToChannel(channel);
    catchup(channel);
  });

  return {
    identifier: LAO_FEATURE_IDENTIFIER,
    components: PublicComponents,
    actionCreators: {
      addLaoServerAddress,
      addNotification,
      discardNotification,
      markNotificationAsRead
    },
    hooks: {
      useLaoList: hooks.LaoHooks.useLaoList,
      useIsLaoOrganizer: hooks.LaoHooks.useIsLaoOrganizer,
      useIsLaoWitness: hooks.LaoHooks.useIsLaoWitness,
      useLaoMap: hooks.LaoHooks.useLaoMap,
      useCurrentLao: hooks.LaoHooks.useCurrentLao,
      useCurrentLaoId: hooks.LaoHooks.useCurrentLaoId,
    },
    functions,
    reducers: {
      ...laoReducer,
      ...notificationReducer
    },
  };
};

export const compose = (config: LaoCompositionConfiguration): LaoCompositionInterface => {
  return {
    identifier: LAO_FEATURE_IDENTIFIER,
    navigation,
    context: {
      EventList: config.EventList,
      encodeLaoConnectionForQRCode: config.encodeLaoConnectionForQRCode,
      laoNavigationScreens: config.laoNavigationScreens,
      organizerNavigationScreens: config.organizerNavigationScreens,
    },
  };
};
