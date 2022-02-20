import { addReducer } from 'core/redux';
import keyPairReducer from './Reducer';

export * from './KeyPairRegistry';
export * from './KeyPairStore';
export * from './Reducer';

export function configureKeyPair() {
  addReducer(keyPairReducer);
}
