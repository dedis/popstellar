import { Chirp, ChirpState } from 'model/objects/Chirp';
import { dispatch, getStore } from '../Storage';
import { addChirp, getChirps } from '../reducers';

export namespace SocialStore {
  /**
   * Stores the new posted chirp.
   *
   * @param chirp
   */
  export function storeChirp(chirp: Chirp): void {
    dispatch(addChirp(chirp.toState()));
  }

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
