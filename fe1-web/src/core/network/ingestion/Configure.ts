import { getStore } from 'core/store';
import { handleRpcRequests, setMessageRegistry } from './Handler';
import { makeMessageStoreWatcher } from './Watcher';
import { getNetworkManager } from '../index';
import { MessageRegistry } from '../jsonrpc/messages';
import { configure } from './index';

export function configureIngestion(messageRegistry: MessageRegistry) {
  // configure the message handlers
  configure(messageRegistry);
  setMessageRegistry(messageRegistry);

  // setup the handler for incoming messages
  getNetworkManager().setRpcHandler(handleRpcRequests);

  // returns the unsubscribe function, which we don't need.
  const store = getStore();
  store.subscribe(makeMessageStoreWatcher(store, messageRegistry));
}
