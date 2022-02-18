import AsyncStorage from '@react-native-async-storage/async-storage';
import { persistCombineReducers } from 'redux-persist';

import { wrapWithClearStorageReducer } from 'core/redux/ClearStorageReducer';
import { AnyAction, Reducer } from 'redux';

const persistConfig = {
  key: 'root',
  storage: AsyncStorage,
};

export function makeRootReducer(reducers: Record<string, Reducer<any>>): Reducer<any, AnyAction> {

  const appReducer = persistCombineReducers(persistConfig, {
    ...reducers,
  });

  return wrapWithClearStorageReducer(appReducer, AsyncStorage);
}
