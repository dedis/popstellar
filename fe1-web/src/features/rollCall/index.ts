// import { addReducer } from 'core/redux';
import { MessageRegistry } from 'core/network/jsonrpc/messages';

import { configureNetwork } from './network';
// import { rollCallReducer } from './reducer';

/**
 * Configures the roll call feature
 *
 * @param registry - The MessageRegistry where we want to add the mappings
 */
export function configure(registry: MessageRegistry) {
  configureNetwork(registry);
  // addReducer(rollCallReducer);
}
