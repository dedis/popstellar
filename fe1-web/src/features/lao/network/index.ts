import { navigationRef } from 'core/navigation/AppNavigation';
import {
  addOnChannelSubscriptionHandlers,
  addOnChannelUnsubscriptionHandlers,
  getNetworkManager,
} from 'core/network';
import { ActionType, MessageRegistry, ObjectType } from 'core/network/jsonrpc/messages';
import { getStore } from 'core/redux';
import { FOUR_SECONDS } from 'resources/const';
import STRINGS from 'resources/strings';

import { getLaoById } from '../functions/lao';
import { resubscribeToLao } from '../functions/network';
import { addSubscribedChannel, removeSubscribedChannel, selectCurrentLaoId } from '../reducer';
import { storeBackendAndConnectToPeers, makeLaoGreetStoreWatcher } from './LaoGreetWatcher';
import {
  handleLaoCreateMessage,
  handleLaoGreetMessage,
  handleLaoStateMessage,
  handleLaoUpdatePropertiesMessage,
} from './LaoHandler';
import { CreateLao, StateLao, UpdateLao } from './messages';
import { GreetLao } from './messages/GreetLao';

export * from './LaoMessageApi';

/**
 * Configures the network callbacks in a MessageRegistry.
 *
 * @param registry - The MessageRegistry where we want to add the mappings
 */
export function configureNetwork(registry: MessageRegistry) {
  registry.add(ObjectType.LAO, ActionType.CREATE, handleLaoCreateMessage, CreateLao.fromJson);
  registry.add(ObjectType.LAO, ActionType.STATE, handleLaoStateMessage, StateLao.fromJson);
  registry.add(
    ObjectType.LAO,
    ActionType.UPDATE_PROPERTIES,
    handleLaoUpdatePropertiesMessage,
    UpdateLao.fromJson,
  );
  registry.add(ObjectType.LAO, ActionType.GREET, handleLaoGreetMessage, GreetLao.fromJson);

  // the lao#greet message can become valid after receiving it if the signature is only added
  // afterwards. listen to state changes that add signatures to lao#greet messages
  const store = getStore();
  store.subscribe(makeLaoGreetStoreWatcher(store, storeBackendAndConnectToPeers));

  // Workaround for https://github.com/dedis/popstellar/issues/1078
  addOnChannelSubscriptionHandlers((laoId, dispatch, channel) =>
    dispatch(addSubscribedChannel(laoId, channel)),
  );

  addOnChannelUnsubscriptionHandlers((laoId, dispatch, channel) =>
    dispatch(removeSubscribedChannel(laoId, channel)),
  );

  // in case of a reconnection, subscribe to and catchup on the LAO channel
  getNetworkManager().addReconnectionHandler(async () => {
    // after reconnecting, check whether we have already been connected to a LAO

    const laoId = selectCurrentLaoId(getStore().getState());
    const lao = getLaoById(laoId?.valueOf() || '');
    if (!laoId || !lao) {
      return;
    }

    await resubscribeToLao(lao, store.dispatch);
  });
  getNetworkManager().addConnectionDeathHandler((address) => {
    const laoId = selectCurrentLaoId(getStore().getState());
    const currentLao = getLaoById(laoId?.valueOf() || '');
    if (!laoId || !currentLao) {
      return;
    }

    if (currentLao.server_addresses.includes(address)) {
      // a connection we have with this lao has been terminated
      // -> navigate back to the home screen
      if (navigationRef.isReady()) {
        if (toast) {
          toast.show(STRINGS.lao_error_disconnect, {
            type: 'danger',
            placement: 'top',
            duration: FOUR_SECONDS,
          });
        }
        navigationRef.navigate(STRINGS.navigation_app_home, {
          screen: STRINGS.navigation_home_home,
        });
      }

      // in the future this can be handled more gracefully if there are multiple concurrent connections
      // with different servers
    }
  });
}
