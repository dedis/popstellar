import { Reducer } from 'redux';

import { persist, store } from './GlobalStore';
import { makeRootReducer } from './RootReducer';

let reducers: Record<string, Reducer<any>> = {};

export function addReducers(newReducers: Record<string, Reducer<any>>) {
  reducers = {
    ...reducers,
    ...newReducers,
  };
  const newRootReducer = makeRootReducer(reducers);

  store.replaceReducer(newRootReducer);
  persist.persist();
}

export function clearReducers() {
  const newRootReducer = makeRootReducer({});
  store.replaceReducer(newRootReducer);
}
