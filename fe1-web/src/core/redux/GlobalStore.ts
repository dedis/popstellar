import { configureStore, ThunkAction, ThunkDispatch } from '@reduxjs/toolkit';
import { Action, AnyAction, applyMiddleware, Store } from 'redux';
import { composeWithDevTools } from '@redux-devtools/extension';
import { FLUSH, PAUSE, PERSIST, Persistor, persistStore, PURGE, REGISTER, REHYDRATE } from 'redux-persist';
import thunkMiddleware from 'redux-thunk';

// Core storage module for the React app
// Import all the reducers, defining how storage is organized and accessed
import { makeRootReducer } from './RootReducer';

// Extend redux dispatch definition to support ThunkActions
// as per https://stackoverflow.com/a/67634381
declare module 'redux' {
  interface Dispatch<A extends Action = AnyAction> {
    <S, E, R>(asyncAction: ThunkAction<R, S, E, A>): R;
  }
}

// Only used at application startup
const noopReducer = {
  noopReducer: (x: any) => x || {},
};

// Initialize the store and expose its configuration
export const store: Store = configureStore({
  reducer: makeRootReducer(noopReducer),
  enhancers: [applyMiddleware(thunkMiddleware)],
  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware({
      serializableCheck: {
        ignoredActions: [FLUSH, REHYDRATE, PAUSE, PERSIST, PURGE, REGISTER],
      },
    }),
});

/**
 * The type of functions that are executed as soon as the redux store is rehydrated
 */
type RehydrationCallback = () => void;

/**
 * A list of functions that are executed as soon as the redux store is rehydrated
 */
const rehydrationCallbacks: RehydrationCallback[] = [];

/**
 * Registers a callback to be executed after the store has been rehydrated
 * @param callback The function that should be called after the redux store has been rehydrated
 */
export const addRehydrationCallback = (callback: RehydrationCallback) => {
  rehydrationCallbacks.push(callback);
};

/**
 * The persistor object that allows the execution of persistence operations
 */
export const persist: Persistor = persistStore(
  store,
  {
    // It seems this options is documented and working but not yet part of the types?
    // See https://github.com/rt2zz/redux-persist#persiststorestore-config-callback
    // @ts-ignore
    manualPersist: true,
  },
  () => {
    // callback function that is called after the store has been hydrated
    // execute all registered callback functions
    rehydrationCallbacks.forEach((callback) => callback());
  },
);

// Expose the access functions
export const getStore = (): Store => store;
export const { dispatch } = store;

// Expose the types needed elsewhere
export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
export type AppThunk<ReturnType = void> = ThunkAction<ReturnType, RootState, unknown, AnyAction>;
export type AsyncDispatch = ThunkDispatch<RootState, void, Action>;
