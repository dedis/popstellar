import { addReducers } from 'core/redux';

import keyPairReducer from './KeyPairReducer';
import { KeyPairStore } from './KeyPairStore';

export * from './KeyPairReducer';
export * from './KeyPairRegistry';
export * from './KeyPairStore';

export function configureKeyPair() {
  addReducers(keyPairReducer);

  // initialize the keypair
  KeyPairStore.get();
}
