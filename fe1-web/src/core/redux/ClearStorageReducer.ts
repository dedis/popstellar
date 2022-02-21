import { AnyAction, Reducer } from 'redux';
import { WebStorage } from 'redux-persist/es/types';

/**
 * Wraps the reducers inside a reducer that allows clearing the storage entirely.
 * This is just a useful debugging tool.
 *
 * @param reducers
 * @param storage
 */
export function wrapWithClearStorageReducer(reducers: Reducer, storage: WebStorage): Reducer {
  return (state: any, action: AnyAction) => {
    // clears the local cached storage as well as the state of the storage
    let newState = state;
    if (action.type === 'CLEAR_STORAGE') {
      // unsafe, asynchronous operation:
      storage.removeItem('persist:root');
      newState = undefined;
    }
    return reducers(newState, action);
  };
}
