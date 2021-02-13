import { persistCombineReducers, persistStore } from 'redux-persist';
import { AnyAction, createStore, Reducer, Store } from 'redux';
import { Persistor, WebStorage } from 'redux-persist/es/types';
import keypairReducer from './Reducers/keypairReducer';
import currentLaoReducer from './Reducers/currentLaoReducer';
import { StorageKeyPair } from './storage/StorageKeyPair';
import { StorageCurrentLao } from "./storage/StorageCurrentLao";

interface StorageConfig {
  store: Store,
  persistor: Persistor,
}

let store: Store;
let persistor: Persistor;

let storageKeyPair: StorageKeyPair;
let storageCurrentLao: StorageCurrentLao;

export function initialise(): void {

  let storage: WebStorage;
  if (typeof localStorage === 'undefined' || localStorage === null) {
    // native local storage available
    const LocalStorage = require('node-localstorage').LocalStorage;
    storage = new LocalStorage('./scratch');
  } else {
    // using a polyfill to replace the missing local storage
    storage = require('redux-persist/lib/storage').localStorage;
  }

  const persistConfig = { key: 'root', storage };

  let appReducer: Reducer = persistCombineReducers(persistConfig, {
    keypairReducer,
    currentLaoReducer,
  });

  // Trick used to clear local persistent storage
  const rootReducer = (state: any, action: AnyAction) => {
    // clears the local cached storage as well as the state of the storage
    let newState = state;
    if (action.type === 'CLEAR_STORAGE') { storage.removeItem('persist:root'); newState = undefined; }
    return appReducer(newState, action);
  };

  // Initiates (opens) the persistent storage
  store = createStore(rootReducer);
  if (typeof localStorage !== 'undefined' && localStorage !== null)
    // persisting the local storage on the polyfill make the tests run forever
    persistor = persistStore(store);

  // Initiates sub-storages
  storageKeyPair = new StorageKeyPair(store);
  storageCurrentLao = new StorageCurrentLao(store);

/*
  let appReducer: Reducer;
  (new Promise((resolve, reject) => {
    appReducer = persistCombineReducers(persistConfig, {
      keypairReducer,
    });
  })).then(
    () => {

      /** Trick used to clear local persistent storage *
      const rootReducer = (state: any, action: AnyAction) => {
        // clears the local cached storage as well as the state of the storage
        let newState = state;
        if (action.type === 'CLEAR_STORAGE') { storage.removeItem('persist:root'); newState = undefined; }
        return appReducer(newState, action);
      };

      /** Initiates (opens) the persistent storage; should be used at top level (App.js) *
      store = createStore(rootReducer);
      persistor = persistStore(store);

      /** Initiates sub-storages *
      storageKeyPair = new StorageKeyPair(store);
      //return { store, persistor };
    },
  );*/
}

export function dispatch(action: AnyAction): void {
  store.dispatch(action);
}

export function getStorageKeyPair(): StorageKeyPair {
  return storageKeyPair;
}

export function getStorageCurrentLao(): StorageCurrentLao {
  return storageCurrentLao;
}
