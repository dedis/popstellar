import { applyMiddleware, Store, createStore, combineReducers } from 'redux';
import thunkMiddleware from 'redux-thunk';
import { walletReducer } from 'features/wallet/reducer';

const reducers = combineReducers({
  ...walletReducer,
});

const store: Store = createStore(reducers, applyMiddleware(thunkMiddleware));

export const getStore = () => store;
export const dispatch: typeof store.dispatch = (x: any) => store.dispatch(x);
