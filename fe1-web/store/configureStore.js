import { createStore } from 'redux';
import { persistStore, persistCombineReducers } from 'redux-persist';
import storage from 'redux-persist/lib/storage';

import { keyPairReducer, openedLaoReducer } from './reducers';
import currentEventsReducer from './reducers/currentEventsReducer';
import connectLAOsReducer from './reducers/connectLAOsReducer';
import openRollCallIDReducer from './reducers/openRollCallIDReducer';

/**
 * Create the redux persistent store for the app
 */

const persistConfig = {
  key: 'root',
  storage,
};

/** Persistent collection of reducers */
const appReducer = persistCombineReducers(persistConfig, {
  currentLaoReducer: openedLaoReducer,
  keyPairReducer,
  currentEventsReducer,
  connectLAOsReducer,
  openRollCallIDReducer,
});

/** Trick used to clear local persistent storage */
const rootReducer = (state, action) => {
  // clears the local cached storage as well as the state of the storage
  let newState = state;
  if (action.type === 'CLEAR_STORAGE') { storage.removeItem('persist:root'); newState = undefined; }
  return appReducer(newState, action);
};

let store;
let persistor;

/** Initiates (opens) the persistent storage; should be used at top level (App.js) */
export const storeInit = () => {
  store = createStore(rootReducer);
  persistor = persistStore(store);
  return { store, persistor };
};

export const getPersistConfig = () => persistConfig;
export const getStore = () => store;
export const getPersistor = () => persistor;
