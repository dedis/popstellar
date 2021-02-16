import { Lao } from 'model/objects';
import { dispatch, getStore } from '../Storage';
import { ActionOpenedLaoReducer } from '../Actions';

export namespace OpenedLaoStore {

  export function store(value: Lao): void {
    dispatch({ type: ActionOpenedLaoReducer.SET_OPENED_LAO, value });
  }

  export function get(): Lao {
    const lao: Lao = getStore().getState().currentLaoReducer;

    return (lao !== null)
      ? lao
      : (() => { throw new Error('Error encountered while accessing storage : no currently opened LAO'); })();
  }
}
