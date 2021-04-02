const initialState = { roll_call_id: '-1' };

// TODO: remove this reducer altogether
function openRollCallIDReducer(state = initialState, action) {
  if (action.type === 'SET_OPEN_ROLL_CALL_ID') {
    const nextState = {
      ...state,
      roll_call_id: action.value,
    };
    return nextState || state;
  }
  return state;
}

export default openRollCallIDReducer;
