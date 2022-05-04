import { KeyPairRegistry } from 'core/keypair/KeyPairRegistry';
import { MessageRegistry, SignatureType } from 'core/network/jsonrpc/messages';

import * as navigation from './navigation';
import { configureNetwork } from './network';
import { getCurrentPopTokenFromStore } from './objects';
import { walletReducer } from './reducer';

/**
 * Configures the wallet feature
 */
export function configure(kpRegistry: KeyPairRegistry, messageRegistry: MessageRegistry) {
  configureNetwork(messageRegistry);
  kpRegistry.add(SignatureType.POP_TOKEN, getCurrentPopTokenFromStore);
  return {
    navigation,
    reducers: {
      ...walletReducer,
    },
  };
}
