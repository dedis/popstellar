const initialState = { lao: {} };

function currentLaoReducer(state = initialState, action) {
  let nextState;
  switch (action.type) {
    case 'SET_CURRENT_LAO':
      nextState = {
        ...state,
        lao: action.value,
      };
      return nextState || state;
    default:
      return state;
  }
}

export default currentLaoReducer;
