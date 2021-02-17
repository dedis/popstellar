import { AnyAction } from 'redux';
import { Lao, LaoState } from 'model/objects';
import { ActionOpenedLaoReducer } from '../Actions';

const initialState: LaoState | null = null;

/**
 * Reducer to store the current opened LAO
 *
 * Action types:
 *  - SET_OPENED_LAO: set a LAO in local storage
 *
 *  Note: action is a JsObject of the form { type: <ACTION_TYPE> } where
 *  <ACTION_TYPE> is a string
 *
 * @param state JsObject containing the LAO to store
 * @param action action to be executed by the reducer
 * @returns new LAO if action is valid, old LAO otherwise
 */
export function openedLaoReducer(state: LaoState | null = initialState, action: AnyAction)
  : LaoState | null {
  try {
    if (action.type === ActionOpenedLaoReducer.SET_OPENED_LAO) {
      if (action.value === undefined || action.value === null) {
        console.log('LAO storage was set to: null');
        return null;
      }

      const lao = Object.freeze(Lao.fromState(action.value).toState());
      console.log(`LAO storage was update with open: ${lao.id.toString()}`);
      return lao;
    }
  } catch (e) {
    console.exception('Could not update opened LAO state due to exception', e);
  }

  return state;
}
