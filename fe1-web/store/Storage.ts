import { persistCombineReducers, persistStore } from 'redux-persist';
import {
  AnyAction, createStore, Reducer, Store,
} from 'redux';
import { Persistor } from 'redux-persist/es/types';
import AsyncStorage from '@react-native-community/async-storage';

import {
  currentEventsReducer, keyPairReducer, openRollCallIDReducer,
  availableLaosReducer, openedLaoReducer,
} from './reducers';

interface PersistStoreConfig {
  store: Store,
  persist: Persistor,
}

let store: Store;
let persist: Persistor;

export function storeInit(): PersistStoreConfig {
  const persistConfig = {
    key: 'root',
    storage: AsyncStorage,
  };

  const appReducer: Reducer = persistCombineReducers(persistConfig, {
    getKeyPair: keyPairReducer,
    getOpenedLao: openedLaoReducer,
    getAvailableLaos: availableLaosReducer,
    getCurrentEvents: currentEventsReducer,
    getOpenRollCallId: openRollCallIDReducer,
  });

  // Trick used to clear local persistent storage
  const rootReducer = (state: any, action: AnyAction) => {
    // clears the local cached storage as well as the state of the storage
    let newState = state;
    if (action.type === 'CLEAR_STORAGE') {
      // unsafe, asynchronous operation:
      AsyncStorage.removeItem('persist:root');
      newState = undefined;
    }
    return appReducer(newState, action);
  };

  // Initiates (opens) the persistent storage
  store = createStore(rootReducer);
  if (typeof localStorage !== 'undefined' && localStorage !== null) {
    // persisting the local storage on the polyfill make the tests run forever
    persist = persistStore(store);
  }

  return { store, persist };
}

export function getStore(): Store {
  return store;
}

export function dispatch(action: AnyAction): void {
  store.dispatch(action);
}
