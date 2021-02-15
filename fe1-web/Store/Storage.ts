import { persistCombineReducers, persistStore } from 'redux-persist';
import {
  AnyAction, createStore, Reducer, Store,
} from 'redux';
import { Persistor, WebStorage } from 'redux-persist/es/types';
import keypairReducer from './Reducers/keypairReducer';
import currentLaoReducer from './Reducers/currentLaoReducer';

let store: Store;
let persistor: Persistor;

export function initialise(): void {
  let storage: WebStorage;
  // FIXME doesnt compile
  //if (!window || typeof window.localStorage === 'undefined' || window.localStorage === null) {
  if (typeof localStorage === 'undefined' || localStorage === null) {
    // using a polyfill to replace the missing local storage
    const { LocalStorage } = require('node-localstorage');
    storage = new LocalStorage('./scratch');
  } else {
    // native local storage available
    storage = require('redux-persist/lib/storage').localStorage;
  }

  const persistConfig = { key: 'root', storage };

  const appReducer: Reducer = persistCombineReducers(persistConfig, {
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
  if (typeof localStorage !== 'undefined' && localStorage !== null) {
    // persisting the local storage on the polyfill make the tests run forever
    persistor = persistStore(store);
  }
}

export function getStore(): Store {
  return store;
}

export function dispatch(action: AnyAction): void {
  store.dispatch(action);
}
