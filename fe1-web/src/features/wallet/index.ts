import { addReducer } from 'core/redux';
import { configurePopTokenSignature } from 'core/network/jsonrpc/messages';
import { walletReducer } from './reducer';
import { getCurrentPopTokenFromStore } from './objects/Token';

/**
 * Configures the wallet feature
 */
export function configure() {
  addReducer(walletReducer);
  configurePopTokenSignature(getCurrentPopTokenFromStore);
}
