import { useSelector } from 'react-redux';
import { Lao } from 'model/objects';
import { dispatch } from '../Storage';
import { laoAdded, makeCurrentLao } from '../reducers';

const currentLao = makeCurrentLao();

export namespace OpenedLaoStore {

  export function store(lao: Lao): void {
    const laoState = lao.toState();
    dispatch(laoAdded(laoState));
  }

  export function get(): Lao {
    const laoState = useSelector(currentLao);

    if (laoState === null || laoState === undefined) {
      throw new Error('Error encountered while accessing storage : no currently opened LAO');
    }

    return Lao.fromState(laoState);
  }
}
