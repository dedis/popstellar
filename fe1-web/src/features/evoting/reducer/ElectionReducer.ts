/**
 * This error is disabled since reducers use the createSlice function, which requires the user to
 * param-reassign. Please do not disable other errors.
 */
/* eslint-disable no-param-reassign */
import { createSelector, createSlice, Draft, PayloadAction } from '@reduxjs/toolkit';

import { Hash } from 'core/objects';

import { Election, ElectionState, EMPTY_QUESTION, QuestionState } from '../objects';

/**
 * Reducer & associated function implementation to store all known elections
 */

export interface ElectionReducerState {
  byId: Record<string, ElectionState>;
  allIds: string[];
  defaultQuestions: QuestionState[];
}

const initialState: ElectionReducerState = {
  byId: {},
  allIds: [],
  defaultQuestions: [EMPTY_QUESTION],
};

export const ELECTION_REDUCER_PATH = 'election';

const electionSlice = createSlice({
  name: ELECTION_REDUCER_PATH,
  initialState,
  reducers: {
    addElection: (state: Draft<ElectionReducerState>, action: PayloadAction<ElectionState>) => {
      const newElection = action.payload;

      if (newElection.id in state.byId) {
        throw new Error(`Tried to add election with id ${newElection.id} but it already exists`);
      }

      state.allIds.push(newElection.id);
      state.byId[newElection.id] = newElection;
    },

    updateElection: (state: Draft<ElectionReducerState>, action: PayloadAction<ElectionState>) => {
      const updatedElection = action.payload;

      if (!(updatedElection.id in state.byId)) {
        throw new Error(`Tried to update inexistent election with id ${updatedElection.id}`);
      }

      state.byId[updatedElection.id] = updatedElection;
    },

    removeElection: (state, action: PayloadAction<Hash>) => {
      const electionId = action.payload.valueOf();

      if (!(electionId in state.byId)) {
        throw new Error(`Tried to delete inexistent election with id ${electionId}`);
      }

      delete state.byId[electionId];
      state.allIds = state.allIds.filter((id) => id !== electionId);
    },
    setDefaultQuestions: (state, action: PayloadAction<QuestionState[]>) => {
      state.defaultQuestions = action.payload;
    },
  },
});

export const { addElection, updateElection, removeElection, setDefaultQuestions } =
  electionSlice.actions;

export const getElectionState = (state: any): ElectionReducerState => state[ELECTION_REDUCER_PATH];

export const electionReduce = electionSlice.reducer;

export default {
  [ELECTION_REDUCER_PATH]: electionSlice.reducer,
};

/**
 * Creates a selector that retrieves an election by its id
 * @param electionId The if of the election / event to retrieve
 * @returns The selector
 */
export const makeElectionSelector = (electionId: Hash) => {
  const electionIdString = electionId.valueOf();

  return createSelector(
    // First input: a map of ids to elections
    (state: any) => getElectionState(state).byId,
    // Selector: returns the selected election
    (electionById: Record<string, ElectionState>): Election | undefined => {
      if (!(electionIdString in electionById)) {
        return undefined;
      }

      return Election.fromState(electionById[electionIdString]);
    },
  );
};

/**
 * Retrieves an election by its id from the redux store
 * @remark This function does not memoize its result, use 'makeElectionSelector' in react components
 * @param electionId The if of the election / event to retrieve
 * @param state The redux state
 * @returns The constructed election or undefined if the id is not found
 */
export const getElectionById = (electionId: Hash, state: unknown) => {
  const electionIdString = electionId.valueOf();
  const electionById = getElectionState(state).byId;

  if (!(electionIdString in electionById)) {
    return undefined;
  }

  return Election.fromState(electionById[electionIdString]);
};
