/**
 * This error is disabled since reducers use the createSlice function, which requires the user to
 * param-reassign. Please do not disable other errors.
 */
/* eslint-disable no-param-reassign */

import { createSelector, createSlice, Draft, PayloadAction } from '@reduxjs/toolkit';

import { Base64UrlData } from 'core/objects';

import { ElectionPublicKey } from '../objects/ElectionPublicKey';

/**
 * Reducer & associated functions to store received election key
 */

export interface ElectionKeyReducerState {
  byElectionId: {
    [electionId: string]: string;
  };
}

const initialState: ElectionKeyReducerState = {
  byElectionId: {},
};

export const ELECTION_KEY_REDUCER_PATH = 'electionKey';

const electionKeySlice = createSlice({
  name: ELECTION_KEY_REDUCER_PATH,
  initialState,
  reducers: {
    addElectionKey: (
      state: Draft<ElectionKeyReducerState>,
      action: PayloadAction<{ electionId: string; electionKey: string }>,
    ) => {
      const { electionId, electionKey } = action.payload;

      if (electionId in state.byElectionId) {
        throw new Error(
          `There is already an election key stored for the election id ${electionId}`,
        );
      }

      state.byElectionId[electionId] = electionKey;
    },

    removeElectionKey: (state, action: PayloadAction<string>) => {
      const electionId = action.payload;

      if (!(electionId in state.byElectionId)) {
        return;
      }

      delete state.byElectionId[electionId];
    },

    clearAllElectionKeys: (state) => {
      state.byElectionId = {};
    },
  },
});

export const { addElectionKey, removeElectionKey, clearAllElectionKeys } = electionKeySlice.actions;

export const electionKeyReduce = electionKeySlice.reducer;

export default {
  [ELECTION_KEY_REDUCER_PATH]: electionKeySlice.reducer,
};

export const getElectionKeyState = (state: any): ElectionKeyReducerState =>
  state[ELECTION_KEY_REDUCER_PATH];

/**
 * A function to directly retrieve the election key from the redux store for a election lao id
 * @remark NOTE: This function does not memoize the result. If you need this, use makeElectionKeySelector instead
 * @param electionId The election id
 * @param state The redux state
 * @returns The election key for the election id or undefined if there is none
 */
export const getElectionKeyByElectionId = (
  electionId: string,
  state: any,
): ElectionPublicKey | undefined => {
  const electionKeyState = getElectionKeyState(state);

  if (electionId in electionKeyState.byElectionId) {
    return new ElectionPublicKey(new Base64UrlData(electionKeyState.byElectionId[electionId]));
  }

  return undefined;
};

/**
 * Creates a election key selector for a given election id. Can for example be used in useSelector()
 * @param electionId The election id
 * @returns A selector for the election key for the given election id or undefined if there is none
 */
export const makeElectionKeySelector = (electionId: string) =>
  createSelector(
    // First input: map of lao ids to servers
    (state) => getElectionKeyState(state).byElectionId,
    // Selector: returns the election key associated to the given election id
    (byElectionId: ElectionKeyReducerState['byElectionId']): ElectionPublicKey | undefined => {
      if (electionId in byElectionId) {
        return new ElectionPublicKey(new Base64UrlData(byElectionId[electionId]));
      }

      return undefined;
    },
  );
