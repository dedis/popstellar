import { addReducer } from 'core/redux';
import keyPairReducer from './KeyPairReducer';
import { KeyPairStore } from './KeyPairStore';

export * from './KeyPairRegistry';
export * from './KeyPairStore';
export * from './KeyPairReducer';

export function configureKeyPair() {
  addReducer(keyPairReducer);

  // initialize the keypair
  KeyPairStore.get();
}
