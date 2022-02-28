import { applyMiddleware, Store, createStore, combineReducers, AnyAction, Reducer } from 'redux';
import thunkMiddleware from 'redux-thunk';

import { walletReducer } from 'features/wallet/reducer';
import { eventsReducer } from 'features/events/reducer';
import { laoReducer } from 'features/lao/reducer';

export function wrapWithClearStorageReducer(reducers: Reducer): Reducer {
  return (state: any, action: AnyAction) => {
    // clears the local cached storage as well as the state of the storage
    let newState = state;
    if (action.type === 'CLEAR_STORAGE') {
      newState = undefined;
    }
    return reducers(newState, action);
  };
}

const reducers = wrapWithClearStorageReducer(
  combineReducers({
    ...laoReducer,
    ...walletReducer,
    ...eventsReducer,
  }),
);

export const store: Store = createStore(reducers, applyMiddleware(thunkMiddleware));

export const getStore = () => store;
export const dispatch: typeof store.dispatch = (x: any) => store.dispatch(x);
