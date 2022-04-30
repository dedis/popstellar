import { KeyPairRegistry } from 'core/keypair/KeyPairRegistry';
import { SignatureType } from 'core/network/jsonrpc/messages';

import * as navigation from './navigation';
import { getCurrentPopTokenFromStore } from './objects';
import { walletReducer } from './reducer';

/**
 * Configures the wallet feature
 */
export function configure(registry: KeyPairRegistry) {
  registry.add(SignatureType.POP_TOKEN, getCurrentPopTokenFromStore);
  return {
    navigation,
    reducers: {
      ...walletReducer,
    },
  };
}
