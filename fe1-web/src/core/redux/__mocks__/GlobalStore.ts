import { configureStore } from '@reduxjs/toolkit';
import { combineReducers, Store } from 'redux';
import thunkMiddleware from 'redux-thunk';

import { walletReducer } from 'features/wallet/reducer';

const reducers = combineReducers({
  ...walletReducer,
});

const store: Store = configureStore({
  reducer: reducers,
  middleware: [thunkMiddleware],
});

export const getStore = () => store;
export const dispatch: typeof store.dispatch = (x: any) => store.dispatch(x);
