import AsyncStorage from '@react-native-community/async-storage';
import { persistCombineReducers } from 'redux-persist';

// not yet fully migrated, remove?
import openRollCallIDReducer from './OpenRollCallIDReducer';

// fully migrated:
import laoReducer from './LaoReducer';
import keyPairReducer from './KeyPairReducer';
import eventsReducer from './EventsReducer';

import { wrapWithClearStorageReducer } from './ClearStorageReducer';

const persistConfig = {
  key: 'root',
  storage: AsyncStorage,
};

const appReducer = persistCombineReducers(persistConfig, {
  ...keyPairReducer,
  ...laoReducer,
  ...eventsReducer,
  openedRollCallId: openRollCallIDReducer,
});

export const rootReducer = wrapWithClearStorageReducer(appReducer, AsyncStorage);
