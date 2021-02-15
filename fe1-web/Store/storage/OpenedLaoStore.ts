import { Lao } from 'Model/Objects';
import { dispatch, getStore } from '../Storage';

export namespace OpenedLaoStore {

  export function store(value: Lao): void {
    dispatch({ type: 'SET_CURRENT_LAO', value });
  }

  export function get(): Lao | null {
    const lao: Lao = getStore().getState().currentLaoReducer;

    return (lao !== null)
      ? new Lao(lao.name, lao.id, lao.creation, lao.last_modified, lao.organizer, lao.witnesses)
      : null;
  }
}
