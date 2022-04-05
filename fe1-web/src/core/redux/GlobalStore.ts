// Core storage module for the React app
import { ThunkAction, ThunkDispatch } from '@reduxjs/toolkit';
import { Action, AnyAction, applyMiddleware, createStore, Store } from 'redux';
import { composeWithDevTools } from 'redux-devtools-extension';
import { Persistor, persistStore } from 'redux-persist';
import thunkMiddleware from 'redux-thunk';

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
const composeEnhancers = composeWithDevTools({ trace: true, traceLimit: 25 });
const composedEnhancer = composeEnhancers(applyMiddleware(thunkMiddleware));
export const store: Store = createStore(makeRootReducer(noopReducer), composedEnhancer);
export const persist: Persistor = persistStore(store);

// Expose the access functions
export const getStore = (): Store => store;
export const { dispatch } = store;

// Expose the types needed elsewhere
export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
export type AppThunk<ReturnType = void> = ThunkAction<ReturnType, RootState, unknown, AnyAction>;
export type AsyncDispatch = ThunkDispatch<RootState, void, Action>;
