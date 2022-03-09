import { walletReducer } from 'features/wallet/reducer';
import { applyMiddleware, combineReducers, createStore, Store } from 'redux';
import thunkMiddleware from 'redux-thunk';

const reducers = combineReducers({
  ...walletReducer,
});

const store: Store = createStore(reducers, applyMiddleware(thunkMiddleware));

export const getStore = () => store;
export const dispatch: typeof store.dispatch = (x: any) => store.dispatch(x);
