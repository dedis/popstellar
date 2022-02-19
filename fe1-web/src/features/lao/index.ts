import { MessageRegistry } from 'core/network/jsonrpc/messages';
import { addReducer } from 'core/redux';

import { configureNetwork } from './network';
import { laoReducer } from './reducer';
import * as components from './components';
import * as hooks from './hooks';
import * as navigation from './navigation';

/**
 * Configures the LAO feature
 *
 * @param registry - The MessageRegistry where we want to add the mappings
 */
export function configure(registry: MessageRegistry) {
  configureNetwork(registry);
  addReducer(laoReducer);

  return {
    components: components,
    hooks: hooks,
    navigation: navigation,
  };
}
