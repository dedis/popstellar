import { addReducer } from '../redux/Manage';
import keyPairReducer from './Reducer';

export * from './Reducer';
export * from './KeyPairStore';

addReducer(keyPairReducer);
