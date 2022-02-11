import { getStore } from 'store';
import { getNetworkManager } from 'network';
import { MessageRegistry } from 'model/network/method/message/data';
import { handleRpcRequests, setMessageRegistry } from './Handler';
import { makeMessageStoreWatcher } from './Watcher';
import { configure } from './handlers';

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
