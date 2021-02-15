import { AnyAction } from 'redux';
import { Lao } from 'model/objects';
import { ActionOpenedLaoReducer } from '../Actions';

const initialState: Lao | null = null;

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
export function openedLaoReducer(state: Lao | null = initialState, action: AnyAction): Lao | null {
  if (action.type === ActionOpenedLaoReducer.SET_OPENED_LAO) {
    return {
      name: action.value.name,
      id: action.value.id,
      creation: action.value.creation,
      last_modified: action.value.last_modified,
      organizer: action.value.organizer,
      witnesses: action.value.witnesses,
    };
  }
  return state;
}
