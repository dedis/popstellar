import { AnyAction } from 'redux';
import laosData from 'res/laoData';
import { Lao } from 'model/objects';

// FIXME: a significantly better storage would be a map (ID) -> Lao

const initialState: Lao[] = laosData;

/**
 * Reducer to manage the LAOs that the user have already connect to
 *
 * Three differents actions:
 *  ADD_LAO: add or update the LAO given in action.value in the LAOs list
 *  REMOVE_LAO: remove the LAO with ID givent in action.value
 *  CLEAR_LAOS: delete all the store LAO
*/

export function availableLaosReducer(state: Lao[] = initialState, action: AnyAction) {
  switch (action.type) {
    case 'ADD_LAO':
      return [...state.filter((lao) => lao.id !== action.value.id), action.value];

    case 'REMOVE_LAO':
      return [...state.filter((lao) => lao.id !== action.value)];

    case 'CLEAR_LAOS':
      return [];

    default:
      return state;
  }
}
