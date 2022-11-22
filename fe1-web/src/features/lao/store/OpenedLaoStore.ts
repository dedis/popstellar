import { dispatch, getStore } from 'core/redux';

import { NoCurrentLaoError } from '../errors/NoCurrentLaoError';
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
    dispatch(setCurrentLao(lao));
  }

  export function get(): Lao {
    const lao = selectCurrentLao(getStore().getState());

    if (!lao) {
      throw new NoCurrentLaoError(
        'Error encountered while accessing storage : no currently opened LAO',
      );
    }

    return lao;
  }
}
