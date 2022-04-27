/**
 * This error is disabled since reducers use the createSlice function, which requires the user to
 * param-reassign. Please do not disable other errors.
 */
/* eslint-disable no-param-reassign */

import { createSelector, createSlice, Draft, PayloadAction } from '@reduxjs/toolkit';

import { Hash } from 'core/objects';

/**
 * Reducer & associated functions to store received election key message id
 */

export interface ElectionKeyMessageReducerState {
  byElectionId: {
    [electionId: string]: string;
  };
}

const initialState: ElectionKeyMessageReducerState = {
  byElectionId: {},
};

export const ELECTION_KEY_MESSAGE_REDUCER_PATH = 'electionKeyMessage';

const electionKeyMessageSlice = createSlice({
  name: ELECTION_KEY_MESSAGE_REDUCER_PATH,
  initialState,
  reducers: {
    addElectionKeyMessage: (
      state: Draft<ElectionKeyMessageReducerState>,
      action: PayloadAction<{ electionId: string; messageId: string }>,
    ) => {
      const { electionId, messageId } = action.payload;

      if (electionId in state.byElectionId) {
        throw new Error(
          `There is already an election key stored for the election id ${electionId}`,
        );
      }

      state.byElectionId[electionId] = messageId;
    },

    removeElectionKeyMessage: (state, action: PayloadAction<string>) => {
      const electionId = action.payload;

      if (!(electionId in state.byElectionId)) {
        return;
      }

      delete state.byElectionId[electionId];
    },

    clearAllElectionKeyMessages: (state) => {
      state.byElectionId = {};
    },
  },
});

export const { addElectionKeyMessage, removeElectionKeyMessage, clearAllElectionKeyMessages } =
  electionKeyMessageSlice.actions;

export const electionKeyReduce = electionKeyMessageSlice.reducer;

export default {
  [ELECTION_KEY_MESSAGE_REDUCER_PATH]: electionKeyMessageSlice.reducer,
};

export const getElectionKeyState = (state: any): ElectionKeyMessageReducerState =>
  state[ELECTION_KEY_MESSAGE_REDUCER_PATH];

/**
 * A function to directly retrieve the election key from the redux store for a election lao id
 * @remark NOTE: This function does not memoize the result. If you need this, use makeServerSelector instead
 * @param electionId The election id
 * @param state The redux state
 * @returns The public key for the given server address or undefined if there is none
 */
export const getElectionKeyMessageIdByElectionId = (
  electionId: string,
  state: any,
): Hash | undefined => {
  const electionKeyState = getElectionKeyState(state);

  if (electionId in electionKeyState.byElectionId) {
    return new Hash(electionKeyState.byElectionId[electionId]);
  }

  return undefined;
};

/**
 * Creates a election key selector for a given election id. Can for example be used in useSelector()
 * @param electionId The election id
 * @returns A selector for the election key for the given electio id or undefined if there is none
 */
export const makeElectionKeyMessageIdSelector = (electionId: string) =>
  createSelector(
    // First input: map of lao ids to servers
    (state) => getElectionKeyState(state).byElectionId,
    // Selector: returns the election key associated to the given election id
    (byElectionId: ElectionKeyMessageReducerState['byElectionId']): Hash | undefined => {
      if (electionId in byElectionId) {
        return new Hash(byElectionId[electionId]);
      }

      return undefined;
    },
  );
