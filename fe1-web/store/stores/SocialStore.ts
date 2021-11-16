import { Chirp, ChirpState } from 'model/objects/Chirp';
import { Hash } from 'model/objects';
import { getStore } from '../Storage';
import { getSocialState } from '../reducers';

export namespace SocialStore {
  /**
   * Returns the list of posted chirps for the given LAO.
   */
  export function getAllChirps(laoId: Hash): Chirp[] {
    if (getSocialState(getStore().getState()) === undefined) {
      return [];
    }

    const socialState: ChirpState[] = getSocialState(getStore().getState())
      .byLaoId[laoId.valueOf()].allChirps;
    return socialState.map((cs: ChirpState) => Chirp.fromState(cs));
  }
}
