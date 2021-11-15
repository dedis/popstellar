import { Chirp, ChirpState } from 'model/objects/Chirp';
import { getStore } from '../Storage';
import { getChirps } from '../reducers';

export namespace SocialStore {
  /**
   * Returns the list of posted chirps.
   */
  export function getAllChirps(): Chirp[] {
    if (getChirps(getStore().getState()) === undefined) {
      return [];
    }
    const socialState: ChirpState[] = getChirps(getStore().getState()).allChirps;
    return socialState.map((cs: ChirpState) => Chirp.fromState(cs));
  }
}
