/**
 * This error is disabled since reducers use the createSlice function, which requires the user to
 * param-reassign. Please do not disable other errors.
 */
/* eslint-disable no-param-reassign */
import { createSelector, createSlice, PayloadAction } from '@reduxjs/toolkit';

import { Hash } from 'core/objects';

import { ChallengeState } from '../objects/Challenge';

export const CHALLENGE_REDUCER_PATH = 'challenge';

export interface ChallengeReducerState {
  byLaoId: Record<string, ChallengeState>;
}

const initialState: ChallengeReducerState = {
  byLaoId: {},
};

const challengeSlice = createSlice({
  name: CHALLENGE_REDUCER_PATH,
  initialState,
  reducers: {
    addChallenge: {
      prepare(laoId: Hash, challenge: ChallengeState) {
        return {
          payload: {
            laoId: laoId.valueOf(),
            challenge: challenge,
          },
        };
      },
      reducer(state, action: PayloadAction<{ laoId: string; challenge: ChallengeState }>) {
        const { laoId, challenge } = action.payload;
        state.byLaoId[laoId] = challenge;
      },
    },
  },
});

export const { addChallenge } = challengeSlice.actions;

export const getChallengeState = (state: any): ChallengeReducerState =>
  state[CHALLENGE_REDUCER_PATH];

/**
 * Retrives a challenge by lao_id
 * @param laoId The id of the lao
 * @returns A challenge state
 */
export const makeChallengeSelector = (laoId: Hash) => {
  return createSelector(
    // First input: a map containing all challenges
    (state: any) => getChallengeState(state),
    // Selector: returns the challenge for a specific lao
    (challengeState: ChallengeReducerState): ChallengeState | undefined => {
      const serializedLaoId = laoId.valueOf();
      if (!challengeState) {
        return undefined;
      }
      return challengeState.byLaoId[serializedLaoId];
    },
  );
};

export const challengeReduce = challengeSlice.reducer;

export default {
  [CHALLENGE_REDUCER_PATH]: challengeSlice.reducer,
};
