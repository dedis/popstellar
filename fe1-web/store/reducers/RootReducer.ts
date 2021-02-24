import AsyncStorage from '@react-native-community/async-storage';
import { persistCombineReducers } from 'redux-persist';
import { keyPairReducer } from './KeyPairReducer';
import { availableLaosReducer } from './AvailableLaosReducer';
import { openedLaoReducer } from './OpenedLaoReducer';

// not yet fully migrated:
import openRollCallIDReducer from './OpenRollCallIDReducer';
import currentEventsReducer from './CurrentEventsReducer';

import { wrapWithClearStorageReducer } from './ClearStorageReducer';

const persistConfig = {
  key: 'root',
  storage: AsyncStorage,
};

const appReducer = persistCombineReducers(persistConfig, {
  keyPair: keyPairReducer,
  availableLaos: availableLaosReducer,
  openedLao: openedLaoReducer,
  currentEvents: currentEventsReducer,
  openedRollCallId: openRollCallIDReducer,
});

export const rootReducer = wrapWithClearStorageReducer(appReducer, AsyncStorage);
