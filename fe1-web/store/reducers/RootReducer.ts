import AsyncStorage from '@react-native-async-storage/async-storage';
import { persistCombineReducers } from 'redux-persist';

import { socialReducer } from 'features/social/reducer';
import eventsReducer from 'features/events/reducer/EventsReducer';
import { walletReducer } from 'features/wallet/reducer';
import laoReducer from './LaoReducer';
import keyPairReducer from './KeyPairReducer';
import messageReducer from './MessageReducer';

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
