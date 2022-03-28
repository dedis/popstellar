import { catchup, getNetworkManager, subscribeToChannel } from 'core/network';
import { ActionType, MessageRegistry, ObjectType } from 'core/network/jsonrpc/messages';
import { getStore } from 'core/redux';

import { getLaoChannel } from '../functions';
import { selectCurrentLaoId } from '../reducer';
import {
  handleLaoCreateMessage,
  handleLaoStateMessage,
  handleLaoUpdatePropertiesMessage,
} from './LaoHandler';
import { CreateLao, StateLao, UpdateLao } from './messages';

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

  // in case of a reconnection, subscribe to and catchup on the LAO channel
  getNetworkManager().addReconnectionHandler(async () => {
    // after reconnecting, check whether we have already been connected to a LAO

    const laoId = selectCurrentLaoId(getStore().getState());
    if (!laoId) {
      return;
    }

    // if yes - then subscribe to the LAO channel and send a catchup
    const channel = getLaoChannel(laoId.valueOf());

    await subscribeToChannel(channel);
    await catchup(channel);
  });
}
