import AsyncStorage from '@react-native-async-storage/async-storage';
import { wrapWithClearStorageReducer } from 'core/redux/ClearStorageReducer';
import { Reducer } from 'redux';
import { persistCombineReducers } from 'redux-persist';

const persistConfig = {
  key: 'root',
  storage: AsyncStorage,
};

export function makeRootReducer(reducers: Record<string, Reducer>): Reducer {
  const appReducer = persistCombineReducers(persistConfig, {
    ...reducers,
  });

  return wrapWithClearStorageReducer(appReducer, AsyncStorage);
}
