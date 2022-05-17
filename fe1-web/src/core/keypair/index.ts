import { addReducers, addRehydrationCallback } from 'core/redux';

import keyPairReducer from './KeyPairReducer';
import { KeyPairStore } from './KeyPairStore';

export * from './KeyPairReducer';
export { default as keyPairReducer } from './KeyPairReducer';

export * from './KeyPairRegistry';
export * from './KeyPairStore';

export function configureKeyPair() {
  addReducers(keyPairReducer);
}

export function initializeKeyPair() {
  // initialize the keypair
  const keyPair = KeyPairStore.get();
  console.log(`Using the public key: ${keyPair.publicKey.toString()}`);
}

// only initialize keypair after the store has been rehydrated, otherwise we get a new key
// on every page load..
addRehydrationCallback(initializeKeyPair);
