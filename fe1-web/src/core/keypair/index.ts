import { addReducer } from '../redux/Manage';
import keyPairReducer from './Reducer';
import { KeyPairStore } from './KeyPairStore';

export * from './KeyPairStore';
export * from './Reducer';

export function configureKeyPair() {
  addReducer(keyPairReducer);
  KeyPairStore.get();
}
