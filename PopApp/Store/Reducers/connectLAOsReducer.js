import laosData from '../../res/laoData';

const initialState = { LAOs: laosData };

/**
 * Reducer to manage the LAOs that the user have already connect to
 *
 * Three differents actions:
 *  ADD_LAO: add or update the LAO given in action.value in the LAOs list
 *  REMOVE_LAO: remove the LAO with ID givent in action.value
 *  CLEAR_LAOS: delete all the store LAO
*/

// FIXME: `|| state` is never going to be evaluated?
function connectLAOsReducer(state = initialState, action) {
  let nextState;
  switch (action.type) {
    case 'ADD_LAO':
      nextState = {
        ...state,
        LAOs: [...state.LAOs.filter((lao) => lao.id !== action.value.id), action.value],
      };
      return nextState || state;
    case 'REMOVE_LAO':
      nextState = {
        ...state,
        LAOs: [...state.LAOs.filter((lao) => lao.id !== action.value)],
      };
      return nextState || state;
    case 'CLEAR_LAOS':
      nextState = {
        ...state,
        LAOs: [],
      };
      return nextState || state;
    default:
      return state;
  }
}

export default connectLAOsReducer;
