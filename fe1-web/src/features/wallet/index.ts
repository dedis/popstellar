import { addReducer } from 'core/redux';
import { KeyPairRegistry } from 'core/keypair/KeyPairRegistry';
import { walletReducer } from './reducer';
import { configureNetwork } from './network';

/**
 * Configures the wallet feature
 */
export function configure(registry: KeyPairRegistry) {
  addReducer(walletReducer);
  configureNetwork(registry);
}
