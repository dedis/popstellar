import { AnyAction } from 'redux';
import { LaoData } from '../objects';

const initialState: LaoData = {
  name: undefined,
  id: undefined,
  creation: undefined,
  last_modified: undefined,
  organizer: undefined,
  witnesses: undefined,
};

/**
 * Reducer to store the current opened LAO
 *
 * Action types:
 *  - SET_CURRENT_LAO: set a LAO in local storage
 *
 *  Note: action is a JsObject of the form { type: <ACTION_TYPE> } where
 *  <ACTION_TYPE> is a string
 *
 * @param state JsObject containing the LAO to store
 * @param action action to be executed by the reducer
 * @returns {{lao: *}|{lao: {}}} new LAO if action is valid, old LAO otherwise
 */
function currentLaoReducer(state: LaoData = initialState, action: AnyAction) {
  if (action.type === 'SET_CURRENT_LAO') {
    const nextState = {
      ...state,
      name: action.value.name,
      id: action.value.id,
      creation: action.value.creation,
      last_modified: action.value.last_modified,
      organizer: action.value.organizer,
      witnesses: action.value.witnesses,
    };
    return nextState || state;
  }
  return state;
}

export default currentLaoReducer;
