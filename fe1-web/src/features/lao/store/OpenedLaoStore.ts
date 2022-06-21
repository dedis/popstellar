import { dispatch, getStore } from 'core/redux';

import { Lao } from '../objects';
import { setCurrentLao, selectCurrentLao } from '../reducer';

/**
 * Access to the currently opened LAO from store.
 *
 * @remarks
 * Consider an alternative way to access the store whenever possible.
 */
export namespace OpenedLaoStore {
  export function store(lao: Lao): void {
    const laoState = lao.toState();
    dispatch(setCurrentLao(laoState));
  }

  export function get(): Lao {
    const lao = selectCurrentLao(getStore().getState());

    if (!lao) {
      throw new Error('Error encountered while accessing storage : no currently opened LAO');
    }

    return lao;
  }
}
