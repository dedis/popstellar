import eventsData from '../../res/EventData';

const initialState = { events: eventsData };

function currentEventsReducer(state = initialState, action) {
  if (action.type === 'SET_CURRENT_EVENTS') {
    const nextState = {
      ...state,
      events: action.value,
    };
    return nextState || state;
  }
  return state;
}

export default currentEventsReducer;
