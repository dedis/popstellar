import { AsyncStorageStatic } from '@react-native-async-storage/async-storage';
import { AnyAction, Reducer } from 'redux';

/**
 * Wraps the reducers inside a reducer that allows clearing the storage entirely.
 * This is just a useful debugging tool.
 *
 * @param reducers
 * @param storage
 */
export function wrapWithClearStorageReducer(
  reducers: Reducer,
  storage: AsyncStorageStatic,
): Reducer {
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
