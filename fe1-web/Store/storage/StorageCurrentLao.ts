import { dispatch } from '../Storage';
import { Store } from 'redux';
import { Lao } from '../../Model/Objects';
import { LaoData } from '../objects';


export class StorageCurrentLao {

  public readonly storage: Store;

  constructor(storage: Store) {
    this.storage = storage;
  }

  public store(value: Lao): void {
    dispatch({
      type: 'SET_CURRENT_LAO',
      value: {
        name: value.name,
        id: value.id,
        creation: value.creation,
        last_modified: value.last_modified,
        organizer: value.organizer,
        witnesses: value.witnesses,
      }
    });
  }

  public getCurrentLao(): Lao {

    let lao: LaoData = this.storage.getState().currentLaoReducer;

    if (lao.name && lao.id && lao.creation && lao.last_modified && lao.organizer && lao.witnesses)
      return new Lao(lao.name, lao.id, lao.creation, lao.last_modified, lao.organizer, lao.witnesses);

    throw new Error('Error encountered while trying to access the current opened LAO : no LAO is opened');
  }
}
