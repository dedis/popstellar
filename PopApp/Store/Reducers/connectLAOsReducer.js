import laosData from '../../res/laoData';

const initialState = { LAOs: laosData };

function connectLAOsReducer(state = initialState, action) {
  if (action.type === 'SET_CONNECT_LAOS') {
    const nextState = {
      ...state,
      LAOs: action.value,
    };
    return nextState || state;
  }
  return state;
}

export default connectLAOsReducer;
