import AsyncStorage from '@react-native-async-storage/async-storage';
import { persistCombineReducers } from 'redux-persist';

import { socialReducer } from 'features/social/reducer';
import { eventsReducer } from 'features/events/reducer';
import { walletReducer } from 'features/wallet/reducer';
import { laoReducer } from 'features/lao/reducer';
import { keyPairReducer, messageReducer } from 'core/reducers';

import { wrapWithClearStorageReducer } from 'core/redux/ClearStorageReducer';

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
