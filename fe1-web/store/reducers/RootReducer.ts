import AsyncStorage from '@react-native-community/async-storage';
import { persistCombineReducers } from 'redux-persist';

// not yet fully migrated:
import openRollCallIDReducer from './OpenRollCallIDReducer';
import currentEventsReducer from './CurrentEventsReducer';

// half-way
import { keyPairReducer } from './KeyPairReducer';

// fully migrated:
import laoReducer from './LaoReducer';

import { wrapWithClearStorageReducer } from './ClearStorageReducer';

const persistConfig = {
  key: 'root',
  storage: AsyncStorage,
};

const appReducer = persistCombineReducers(persistConfig, {
  keyPair: keyPairReducer,
  laos: laoReducer,
  currentEvents: currentEventsReducer,
  openedRollCallId: openRollCallIDReducer,
});

export const rootReducer = wrapWithClearStorageReducer(appReducer, AsyncStorage);
