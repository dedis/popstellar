import { addReducer } from 'core/redux';
import { walletReducer } from './reducer';

/**
 * Configures the wallet feature
 */
export function configure() {
  addReducer(walletReducer);
}
