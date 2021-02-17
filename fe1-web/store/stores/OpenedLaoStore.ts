import { Lao, LaoState } from 'model/objects';
import { dispatch, getStore } from '../Storage';
import { ActionOpenedLaoReducer } from '../Actions';

export namespace OpenedLaoStore {

  export function store(lao: Lao): void {
    dispatch({
      type: ActionOpenedLaoReducer.SET_OPENED_LAO,
      value: lao.toState(),
    });
  }

  export function get(): Lao {
    const laoState: LaoState = getStore().getState().openedLaoReducer;

    if (laoState === null || laoState === undefined) {
      throw new Error('Error encountered while accessing storage : no currently opened LAO');
    }

    return Lao.fromState(laoState);
  }
}
