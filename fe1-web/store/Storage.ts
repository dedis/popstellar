import { persistStore } from 'redux-persist';
import {
  AnyAction, createStore, Store, applyMiddleware,
} from 'redux';
import thunkMiddleware from 'redux-thunk';
import { composeWithDevTools } from 'redux-devtools-extension';
import { Persistor } from 'redux-persist/es/types';

import rootReducer from './reducers';
import { SimpleWalletStore } from './stores/SimpleWalletStore';

interface PersistStoreConfig {
  store: Store,
  persist: Persistor,
}

let store: Store;
let persist: Persistor;
let walletStore: SimpleWalletStore;

export function storeInit(): PersistStoreConfig {
  const composedEnhancer = composeWithDevTools(applyMiddleware(thunkMiddleware));

  // Initiates (opens) the persistent storage
  store = createStore(rootReducer, composedEnhancer);

  persist = persistStore(store);

  return { store, persist };
}

export function walletStoreInit() {
  walletStore = new SimpleWalletStore('walletDatabase');
}

export function getStore(): Store {
  return store;
}

export function getWalletStore(): SimpleWalletStore {
  return walletStore;
}

export function dispatch(action: AnyAction): void {
  store.dispatch(action);
}
