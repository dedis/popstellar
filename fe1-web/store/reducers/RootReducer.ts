import AsyncStorage from '@react-native-async-storage/async-storage';
import { persistCombineReducers } from 'redux-persist';

import laoReducer from './LaoReducer';
import keyPairReducer from './KeyPairReducer';
import eventsReducer from './EventsReducer';
import messageReducer from './MessageReducer';
import walletReducer from './WalletReducer';
import socialReducer from './SocialReducer';

import { wrapWithClearStorageReducer } from './ClearStorageReducer';

const persistConfig = {
  key: 'root',
  storage: AsyncStorage,
};

const appReducer = persistCombineReducers(persistConfig, {
  ...keyPairReducer,
  ...laoReducer,
  ...eventsReducer,
  ...messageReducer,
  ...walletReducer,
  ...socialReducer,
});

export const rootReducer = wrapWithClearStorageReducer(appReducer, AsyncStorage);
