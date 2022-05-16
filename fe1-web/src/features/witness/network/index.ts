import { ActionType, ObjectType } from 'core/network/jsonrpc/messages';
import { getStore } from 'core/redux';

import { WitnessConfiguration } from '../interface';
import { WitnessMessage } from './messages';
import { handleWitnessMessage } from './WitnessHandler';
import { afterMessageProcessingHandler, makeWitnessStoreWatcher } from './WitnessStoreWatcher';

/**
 * Configures the network callbacks in a MessageRegistry.
 *
 * @param config - The witness feature configuration object
 */
export const configureNetwork = (config: WitnessConfiguration) => {
  config.messageRegistry.add(
    ObjectType.MESSAGE,
    ActionType.WITNESS,
    handleWitnessMessage(config.getCurrentLao),
    WitnessMessage.fromJson,
  );

  // listen for new processable messages
  const store = getStore();
  store.subscribe(
    makeWitnessStoreWatcher(
      store,
      config.getCurrentLaoId,
      afterMessageProcessingHandler(config.enabled, config.addNotification, config.getCurrentLao),
    ),
  );
};
