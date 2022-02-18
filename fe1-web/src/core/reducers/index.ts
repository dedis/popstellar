import { addReducer } from '../redux/Manage';
import keyPairReducer from './KeyPairReducer';

export * from './KeyPairReducer';

addReducer(keyPairReducer);
