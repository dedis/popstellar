import { Lao } from 'model/objects';
import { dispatch, getStore } from '../Storage';
import { connectToLao, makeCurrentLao } from '../reducers';

/**
 * Access to the currently opened LAO from store.
 *
 * @remarks
 * Consider an alternative way to access the store whenever possible.
 */
export namespace OpenedLaoStore {
  let currentLao: any;

  export function store(lao: Lao): void {
    const laoState = lao.toState();
    dispatch(connectToLao(laoState));
  }

  export function get(): Lao {
    if (currentLao === undefined) {
      currentLao = makeCurrentLao();
    }
    const lao = currentLao(getStore().getState());
    if (!lao) {
      throw new Error('Error encountered while accessing storage : no currently opened LAO');
    }
    return lao;
  }
}
