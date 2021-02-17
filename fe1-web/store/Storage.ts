import { persistCombineReducers, persistStore } from 'redux-persist';
import {
  AnyAction, createStore, Reducer, Store,
} from 'redux';
import { Persistor, WebStorage } from 'redux-persist/es/types';

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
  let storage: WebStorage;
  // FIXME doesnt compile
  if (typeof window === 'undefined'
    || typeof window.localStorage === 'undefined'
    || window.localStorage === null) {
    const { LocalStorage } = require('node-localstorage');
    storage = new LocalStorage('./scratch');
  } else {
    // native local storage available
    storage = require('redux-persist/lib/storage').localStorage;
  }

  const persistConfig = { key: 'root', storage };

  const appReducer: Reducer = persistCombineReducers(persistConfig, {
    keyPairReducer,
    availableLaosReducer,
    openedLaoReducer,
    currentEventsReducer,
    openRollCallIDReducer,
  });

  // Trick used to clear local persistent storage
  const rootReducer = (state: any, action: AnyAction) => {
    // clears the local cached storage as well as the state of the storage
    let newState = state;
    if (action.type === 'CLEAR_STORAGE') {
      storage.removeItem('persist:root'); newState = undefined;
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
