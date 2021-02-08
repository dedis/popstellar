const initialState = { lao: {} };

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
function currentLaoReducer(state = initialState, action) {
  if (action.type === 'SET_CURRENT_LAO') {
    const nextState = {
      ...state,
      lao: action.value,
    };
    return nextState || state;
  }
  return state;
}

export default currentLaoReducer;
