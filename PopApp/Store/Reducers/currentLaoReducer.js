const initialState = { lao: {} };

function currentLaoReducer(state = initialState, action) {
  if (action.type === 'SET_CURRENT_LAO') {
    let nextState = {
      ...state,
      lao: action.value,
    };
    return nextState || state;

  } else {
    return state;
  }
}

export default currentLaoReducer;
