import { getStore } from 'store';
import { getNetworkManager } from 'network';
import { messageRegistry } from 'App';
import { handleRpcRequests } from './Handler';
import { makeMessageStoreWatcher } from './Watcher';
import { configure } from './handlers';

export function configureIngestion() {
  // setup the handler for incoming messages
  getNetworkManager().setRpcHandler(handleRpcRequests);

  // configure the message registry
  configure(messageRegistry);

  // returns the unsubscribe function, which we don't need.
  const store = getStore();
  store.subscribe(makeMessageStoreWatcher(store));
}
