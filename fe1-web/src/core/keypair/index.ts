import { addReducer } from 'core/redux';
import keyPairReducer from './Reducer';

export * from './Reducer';
export * from './KeyPairStore';

addReducer(keyPairReducer);
