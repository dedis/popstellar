import { getStore } from '../../redux';
import { getNetworkManager } from '../NetworkManager';
import { MessageRegistry } from '../jsonrpc/messages';
import { handleRpcRequests, setMessageRegistry } from './Handler';
import { makeMessageStoreWatcher } from './Watcher';

/**
 * Configures all handlers of the system within a MessageRegistry.
 *
 * @param registry - The MessageRegistry where we want to add the mappings
 */
export function configure(registry: MessageRegistry) {
  // configure the message handlers
  setMessageRegistry(registry);

  // setup the handler for incoming messages
  getNetworkManager().setRpcHandler(handleRpcRequests);

  // returns the unsubscribe function, which we don't need.
  const store = getStore();
  store.subscribe(makeMessageStoreWatcher(store, registry));
}
