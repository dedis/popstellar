import { getStore } from 'store';
import { getNetworkManager } from 'core/network/index';
import { MessageRegistry } from 'core/network/messages';
import { handleRpcRequests, setMessageRegistry } from 'core/network/ingestion/Handler';
import { makeMessageStoreWatcher } from 'core/network/ingestion/Watcher';
import { configure } from '../../../ingestion/handlers';

export function configureIngestion(messageRegistry: MessageRegistry) {
  // configure the messages handlers
  configure(messageRegistry);
  setMessageRegistry(messageRegistry);

  // setup the handler for incoming messages
  getNetworkManager().setRpcHandler(handleRpcRequests);

  // returns the unsubscribe function, which we don't need.
  const store = getStore();
  store.subscribe(makeMessageStoreWatcher(store, messageRegistry));
}
