import { KeyPairRegistry } from 'core/keypair/KeyPairRegistry';
import { SignatureType } from 'core/network/jsonrpc/messages';

import { getCurrentPopTokenFromStore } from './objects';
import { walletReducer } from './reducer';
import * as navigation from './navigation';

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
