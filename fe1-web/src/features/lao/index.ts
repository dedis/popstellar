import { catchup, getNetworkManager, subscribeToChannel } from 'core/network';
import { MessageRegistry } from 'core/network/jsonrpc/messages';
import { getStore } from 'core/redux';
import { validateLaoId } from 'features/connect/screens/ConnectConfirm';

import { PublicComponents } from './components';
import * as functions from './functions';
import * as hooks from './hooks';
import * as navigation from './navigation';
import { configureNetwork } from './network';
import { laoReducer, selectCurrentLaoId } from './reducer';

/**
 * Configures the LAO feature
 *
 * @param registry - The MessageRegistry where we want to add the mappings
 */
export function configure(registry: MessageRegistry) {
  configureNetwork(registry);

  // in case of a reconnection, send a catchup message on the root channel
  getNetworkManager().addReconnectionHandler(async () => {
    // after reconnecting, check whether we have already been connected to a LAO

    const laoId = selectCurrentLaoId(getStore().getState());
    if (!laoId) {
      return;
    }

    // if yes - then subscribe to the LAO channel and send a catchup
    const channel = validateLaoId(laoId.valueOf());
    if (!channel) {
      throw new Error(`Cannot find the channel corresponding to the LAO id ${laoId.valueOf()}`);
    }
    await subscribeToChannel(channel);
    catchup(channel);
  });

  return {
    components: PublicComponents,
    hooks,
    functions,
    navigation,
    reducers: {
      ...laoReducer,
    },
  };
}
