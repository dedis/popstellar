import { persistStore } from 'redux-persist';
import {
  AnyAction, createStore, Store, applyMiddleware,
} from 'redux';
import thunkMiddleware from 'redux-thunk';
import { composeWithDevTools } from 'redux-devtools-extension';
import { Persistor } from 'redux-persist/es/types';

import rootReducer from './reducers';

interface PersistStoreConfig {
  store: Store,
  persist: Persistor,
}

let store: Store;
let persist: Persistor;

export function storeInit(): PersistStoreConfig {
  const composedEnhancer = composeWithDevTools(applyMiddleware(thunkMiddleware));

  // Initiates (opens) the persistent storage
  store = createStore(rootReducer, composedEnhancer);

  persist = persistStore(store);

  return { store, persist };
}

export function getStore(): Store {
  return store;
}

export function dispatch(action: AnyAction): void {
  store.dispatch(action);
}
