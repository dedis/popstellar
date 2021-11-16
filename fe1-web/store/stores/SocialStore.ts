import { Chirp } from 'model/objects/Chirp';
import { makeChirpsMap } from 'store/reducers';
import { Hash } from 'model/objects';
import { getStore } from '../Storage';

export namespace SocialStore {
  /**
   * Returns the list of posted chirps.
   */
  export function getChirp(id: Hash): Chirp | undefined {
    const chirpsMap = makeChirpsMap();
    const chirps = chirpsMap(getStore().getState());

    if (!(id.valueOf() in chirps)) {
      return undefined;
    }

    return chirps[id.valueOf()];
  }
}
