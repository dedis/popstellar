import { Reducer } from 'redux';
import { store } from './GlobalStore';
import { makeRootReducer } from './RootReducer';

let reducers: Record<string, Reducer<any>> = {};

export function addReducer(newReducers: Record<string, Reducer<any>>) {
  reducers = {
    ...reducers,
    ...newReducers,
  };
  const newRootReducer = makeRootReducer(reducers);

  store.replaceReducer(newRootReducer);
}

export function clearReducers() {
  const newRootReducer = makeRootReducer({});
  store.replaceReducer(newRootReducer);
}
