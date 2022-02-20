import { addReducer } from 'core/redux';
import { KeyPairRegistry } from 'core/keypair/KeyPairRegistry';
import { SignatureType } from 'core/network/jsonrpc/messages';

import { getCurrentPopTokenFromStore } from './objects';
import { walletReducer } from './reducer';

/**
 * Configures the wallet feature
 */
export function configure(registry: KeyPairRegistry) {
  addReducer(walletReducer);
  registry.add(SignatureType.POP_TOKEN, getCurrentPopTokenFromStore);
}
