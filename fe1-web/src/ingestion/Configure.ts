import { getStore } from 'store';
import { getNetworkManager } from 'network';
import { handleRpcRequests } from './Handler';
import { makeMessageStoreWatcher } from './Watcher';

export function configureIngestion() {
  // setup the handler for incoming messages
  getNetworkManager().setRpcHandler(handleRpcRequests);

  // returns the unsubscribe function, which we don't need.
  const store = getStore();
  store.subscribe(makeMessageStoreWatcher(store));
}
