/**
 * This error is disabled since reducers use the createSlice function, which requires the user to
 * param-reassign. Please do not disable other errors.
 */
/* eslint-disable no-param-reassign */

import { createSelector, createSlice, Draft, PayloadAction } from '@reduxjs/toolkit';

import { Hash, PublicKey } from 'core/objects';

/**
 * Reducer & associated functions to store received election keys
 */

export interface ElectionKeyReducerState {
  byElectionId: {
    [electionId: string]: {
      electionKey: string;
      messageId: string;
    };
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
      action: PayloadAction<{ electionId: string; electionKey: string; messageId: string }>,
    ) => {
      const { electionId, electionKey, messageId } = action.payload;

      if (electionId in state.byElectionId) {
        throw new Error(
          `There is already an election key stored for the election id ${electionId}`,
        );
      }

      state.byElectionId[electionId] = {
        electionKey,
        messageId,
      };
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
 * @remark NOTE: This function does not memoize the result. If you need this, use makeServerSelector instead
 * @param electionId The election id
 * @param state The redux state
 * @returns The public key for the given server address or undefined if there is none
 */
export const getElectionKeyByElectionId = (
  electionId: string,
  state: any,
): { electionKey: PublicKey; messageId: Hash } | undefined => {
  const electionKeyState = getElectionKeyState(state);

  if (electionId in electionKeyState.byElectionId) {
    return {
      electionKey: new PublicKey(electionKeyState.byElectionId[electionId].electionKey),
      messageId: new Hash(electionKeyState.byElectionId[electionId].messageId),
    };
  }

  return undefined;
};

/**
 * Creates a election key selector for a given election id. Can for example be used in useSelector()
 * @param electionId The election id
 * @returns A selector for the election key for the given electio id or undefined if there is none
 */
export const makeElectionKeySelector = (electionId: string) =>
  createSelector(
    // First input: map of lao ids to servers
    (state) => getElectionKeyState(state).byElectionId,
    // Selector: returns the election key associated to the given election id
    (
      byElectionId: ElectionKeyReducerState['byElectionId'],
    ): { electionKey: PublicKey; messageId: Hash } | undefined => {
      if (electionId in byElectionId) {
        return {
          electionKey: new PublicKey(byElectionId[electionId].electionKey),
          messageId: new Hash(byElectionId[electionId].messageId),
        };
      }

      return undefined;
    },
  );
