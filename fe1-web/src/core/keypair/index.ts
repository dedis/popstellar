import { addReducer } from 'core/redux';
import keyPairReducer from './KeyPairReducer';

export * from './KeyPairRegistry';
export * from './KeyPairStore';
export * from './KeyPairReducer';

export function configureKeyPair() {
  addReducer(keyPairReducer);
}
