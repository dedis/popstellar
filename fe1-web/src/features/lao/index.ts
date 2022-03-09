import { MessageRegistry } from 'core/network/jsonrpc/messages';

import { PublicComponents } from './components';
import * as hooks from './hooks';
import * as navigation from './navigation';
import { configureNetwork } from './network';
import { laoReducer } from './reducer';

/**
 * Configures the LAO feature
 *
 * @param registry - The MessageRegistry where we want to add the mappings
 */
export function configure(registry: MessageRegistry) {
  configureNetwork(registry);

  return {
    components: PublicComponents,
    hooks: hooks,
    navigation: navigation,
    reducers: {
      ...laoReducer,
    },
  };
}
