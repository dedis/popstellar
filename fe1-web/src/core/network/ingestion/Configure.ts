import * as ChirpHandler from 'features/social/network/ChirpHandler';
import * as ReactionHandler from 'features/social/network/ReactionHandler';
import * as MeetingHandler from 'features/meeting/network/MeetingHandler';
import * as ElectionHandler from 'features/evoting/network/ElectionHandler';
import * as RollCallHandler from 'features/rollCall/network/RollCallHandler';
import * as LaoHandler from 'features/lao/network/LaoHandler';
import * as WitnessHandler from 'features/witness/network/WitnessHandler';

import { getStore } from '../../redux';
import { getNetworkManager } from '../NetworkManager';
import { MessageRegistry } from '../jsonrpc/messages';
import { handleRpcRequests, setMessageRegistry } from './Handler';
import { makeMessageStoreWatcher } from './Watcher';

type ConfigurableHandler = {
  configure: (msg: MessageRegistry) => void;
};

const handlers: Array<ConfigurableHandler> = [
  LaoHandler,
  MeetingHandler,
  RollCallHandler,
  ElectionHandler,
  WitnessHandler,
  ChirpHandler,
  ReactionHandler,
];

/**
 * Configures all handlers of the system within a MessageRegistry.
 *
 * @param registry - The MessageRegistry where we want to add the mappings
 */
export function configure(registry: MessageRegistry) {
  // configure the message handlers
  setMessageRegistry(registry);

  handlers.forEach((h: ConfigurableHandler) => h.configure(registry));

  // setup the handler for incoming messages
  getNetworkManager().setRpcHandler(handleRpcRequests);

  // returns the unsubscribe function, which we don't need.
  const store = getStore();
  store.subscribe(makeMessageStoreWatcher(store, registry));
}
