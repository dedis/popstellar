/**
 * This error is disabled since reducers use the createSlice function, which requires the user to
 * param-reassign. Please do not disable other errors.
 */
/* eslint-disable no-param-reassign */
import { createSelector, createSlice, PayloadAction } from '@reduxjs/toolkit';

import { Hash, PublicKey } from 'core/objects';

import { ChallengeState } from '../objects/Challenge';

export const CHALLENGE_REDUCER_PATH = 'challenge';

export interface ChallengeReducerState {
  byLaoId: Record<string, ChallengeState>;
  recvChallenges: Record<string, [ChallengeState, PublicKey?][]>;
}

const initialState: ChallengeReducerState = {
  byLaoId: {},
  recvChallenges: {},
};

const challengeSlice = createSlice({
  name: CHALLENGE_REDUCER_PATH,
  initialState,
  reducers: {
    setChallenge: {
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
    addReceivedChallenge: {
      prepare(laoId: Hash, challenge: ChallengeState, publicKey?: PublicKey) {
        return {
          payload: {
            laoId: laoId.valueOf(),
            challenge: challenge,
            publicKey: publicKey,
          },
        };
      },
      reducer(
        state,
        action: PayloadAction<{ laoId: string; challenge: ChallengeState; publicKey?: PublicKey }>,
      ) {
        const { laoId, challenge, publicKey } = action.payload;
        if (state.recvChallenges[laoId] === undefined) {
          state.recvChallenges[laoId] = [];
        }
        if (
          state.recvChallenges[laoId].find(
            ([challenge1]) =>
              challenge1.value.valueOf() === challenge.value.valueOf() &&
              challenge1.valid_until.valueOf() === challenge.valid_until.valueOf(),
          )
        ) {
          return;
        }
        state.recvChallenges[laoId].push([challenge, publicKey]);
      },
    },
    removeReceivedChallenge: {
      prepare(laoId: Hash, challenge: ChallengeState, publicKey?: PublicKey) {
        return {
          payload: {
            laoId: laoId.valueOf(),
            challenge: challenge,
            publicKey: publicKey,
          },
        };
      },
      reducer(
        state,
        action: PayloadAction<{ laoId: string; challenge: ChallengeState; publicKey?: PublicKey }>,
      ) {
        const { laoId, challenge } = action.payload;
        if (state.recvChallenges[laoId] === undefined) {
          return;
        }

        state.recvChallenges[laoId] = state.recvChallenges[laoId].filter(
          ([challenge1]) =>
            !(
              challenge1.valid_until.valueOf() === challenge.valid_until.valueOf() &&
              challenge1.value.valueOf() === challenge.value.valueOf()
            ),
        );
      },
    },
  },
});

export const { setChallenge, addReceivedChallenge, removeReceivedChallenge } =
  challengeSlice.actions;

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

/**
 * Retrives all received challenges from a lao
 * @param laoId The id of the lao
 * @returns Array of challenges and publickeys
 */
export const makeChallengeReceveidSelector = (laoId: Hash) => {
  return createSelector(
    // First input: a map containing all challenges
    (state: any) => getChallengeState(state),
    // Selector: returns the challenge for a specific lao
    (challengeState: ChallengeReducerState): [ChallengeState, PublicKey?][] | undefined => {
      const serializedLaoId = laoId.valueOf();
      if (!challengeState) {
        return undefined;
      }
      return challengeState.recvChallenges[serializedLaoId];
    },
  );
};

export const challengeReduce = challengeSlice.reducer;

export default {
  [CHALLENGE_REDUCER_PATH]: challengeSlice.reducer,
};
