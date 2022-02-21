import { addReducers, getStore } from 'core/redux';
import { JsonRpcHandler } from '../RpcHandler';
import { MessageRegistry } from '../jsonrpc/messages';
import { handleRpcRequests, setMessageRegistry } from './Handler';
import { makeMessageStoreWatcher } from './Watcher';
import messageReducer from './MessageReducer';

/**
 * Configures all handlers of the system within a MessageRegistry.
 *
 * @param registry
 */
export function configureIngestion(
  registry: MessageRegistry,
  setRpcHandler: (r: JsonRpcHandler) => void,
) {
  // configure the message handlers
  setMessageRegistry(registry);

  // configure the message reducer
  addReducers(messageReducer);

  // setup the handler for incoming messages
  setRpcHandler(handleRpcRequests);

  // returns the unsubscribe function, which we don't need.
  const store = getStore();
  store.subscribe(makeMessageStoreWatcher(store, registry));
}
