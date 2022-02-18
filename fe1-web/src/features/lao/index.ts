import { MessageRegistry } from 'core/network/jsonrpc/messages';
import { addReducer } from 'core/redux';

import { configureNetwork } from './network';
import { laoReducer } from './reducer';

/**
 * Configures the LAO feature
 *
 * @param registry - The MessageRegistry where we want to add the mappings
 */
export function configure(registry: MessageRegistry) {
  configureNetwork(registry);
  addReducer(laoReducer);
}
