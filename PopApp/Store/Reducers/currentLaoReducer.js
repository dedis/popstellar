const initialState = { lao: {} };

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
