import { createStore } from 'redux';
import { persistStore, persistCombineReducers } from 'redux-persist'
import AsyncStorage from '@react-native-community/async-storage';

import toggleAppNavigationScreen from './Reducers/appToggleReducer';
import currentEventsReducer from './Reducers/currentEventsReducer';
import currentLaoReducer from './Reducers/currentLaoReducer';
import keypairReducer from "./Reducers/keypairReducer";
import connectLAOsReducer from './Reducers/connectLAOsReducer';

/**
 * Create the redux persistent store for the app
 */

const persistConfig = {
  key: 'root',
  storage: AsyncStorage,
};

/** Persistent collection of reducers */
const appReducer = persistCombineReducers(persistConfig, {
  toggleAppNavigationScreenReducer: toggleAppNavigationScreen,
  currentLaoReducer: currentLaoReducer,
  keypairReducer: keypairReducer,
  currentEventsReducer: currentEventsReducer,
  connectLAOsReducer: connectLAOsReducer,
});

/** Trick used to clear local persistent storage */
const rootReducer = (state, action) => {
  // clears the local cached storage as well as the state of the storage
  if (action.type === 'CLEAR_STORAGE') { AsyncStorage.removeItem('persist:root'); state = undefined; }
  return appReducer(state, action);
};


let store;
let persistor;

/** Initiates (opens) the persistent storage; should be used at top level (App.js) */
export const storeInit = () => {
  store = createStore(rootReducer);
  persistor = persistStore(store);
  return { store, persistor }
};

export const getPersistConfig = () => persistConfig;
export const getStore = () => store;
export const getPersistor = () => persistor;
