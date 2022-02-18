// Core storage module for the React app
import { AnyAction, createStore, Store, applyMiddleware, Action } from 'redux';
import { persistStore, Persistor } from 'redux-persist';
import thunkMiddleware from 'redux-thunk';
import { ThunkAction, ThunkDispatch } from '@reduxjs/toolkit';
import { composeWithDevTools } from 'redux-devtools-extension';

// Import all the reducers, defining how storage is organized and accessed
import { makeRootReducer } from './RootReducer';

// Extend redux dispatch definition to support ThunkActions
// as per https://stackoverflow.com/a/67634381
declare module 'redux' {
  interface Dispatch<A extends Action = AnyAction> {
    <S, E, R>(asyncAction: ThunkAction<R, S, E, A>): R;
  }
}

// Initialize the store and expose its configuration
const composedEnhancer = composeWithDevTools(applyMiddleware(thunkMiddleware));
export const store: Store = createStore(makeRootReducer({}), composedEnhancer);
export const persist: Persistor = persistStore(store);

// Expose the access functions
export const getStore = (): Store => store;
export const { dispatch } = store;

// Expose the types needed elsewhere
export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
export type AppThunk<ReturnType = void> = ThunkAction<ReturnType, RootState, unknown, AnyAction>;
export type AsyncDispatch = ThunkDispatch<RootState, void, Action>;
