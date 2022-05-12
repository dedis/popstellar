/**
 * This error is disabled since reducers use the createSlice function, which requires the user to
 * param-reassign. Please do not disable other errors.
 */
/* eslint-disable no-param-reassign */
import { createSelector, createSlice, Draft, PayloadAction } from '@reduxjs/toolkit';

import { Hash } from 'core/objects';

import { RollCall, RollCallState } from '../objects';

/**
 * Reducer & associated function implementation to store all known rollcalls
 */

export interface RollCallReducerState {
  byId: Record<string, RollCallState>;
  allIds: string[];
}

const initialState: RollCallReducerState = {
  byId: {},
  allIds: [],
};

export const ROLLCALL_REDUCER_PATH = 'rollcall';

const rollcallSlice = createSlice({
  name: ROLLCALL_REDUCER_PATH,
  initialState,
  reducers: {
    addRollCall: (state: Draft<RollCallReducerState>, action: PayloadAction<RollCallState>) => {
      const newRollCall = action.payload;

      if (newRollCall.id in state.byId) {
        throw new Error(`Tried to add rollcall with id ${newRollCall.id} but it already exists`);
      }

      state.allIds.push(newRollCall.id);
      state.byId[newRollCall.id] = newRollCall;
    },

    updateRollCall: (state: Draft<RollCallReducerState>, action: PayloadAction<RollCallState>) => {
      const updatedRollCall = action.payload;

      if (!(updatedRollCall.id in state.byId)) {
        throw new Error(`Tried to update inexistent rollcall with id ${updatedRollCall.id}`);
      }

      state.byId[updatedRollCall.id] = updatedRollCall;
    },

    removeRollCall: (state, action: PayloadAction<Hash | string>) => {
      const rollcallId = action.payload.valueOf();

      if (!(rollcallId in state.byId)) {
        throw new Error(`Tried to delete inexistent rollcall with id ${rollcallId}`);
      }

      delete state.byId[rollcallId];
      state.allIds = state.allIds.filter((id) => id !== rollcallId);
    },
  },
});

export const { addRollCall, updateRollCall, removeRollCall } = rollcallSlice.actions;

export const getRollCallState = (state: any): RollCallReducerState => state[ROLLCALL_REDUCER_PATH];

export const rollcallReduce = rollcallSlice.reducer;

export default {
  [ROLLCALL_REDUCER_PATH]: rollcallSlice.reducer,
};

/**
 * Creates a selector that retrieves an rollcall by its id
 * @param rollcallId The if of the rollcall / event to retrieve
 * @returns The selector
 */
export const makeRollCallSelector = (rollcallId: string) =>
  createSelector(
    // First input: map from ids to rollcalls
    (state) => getRollCallState(state).byId,
    // Selector: returns the selected rollcall
    (rollcallById: Record<string, RollCallState>): RollCall | undefined => {
      if (!(rollcallId in rollcallById)) {
        return undefined;
      }

      return RollCall.fromState(rollcallById[rollcallId]);
    },
  );

/**
 * Retrieves an rollcall by its id from the redux store
 * @remark This function does not memoize its result, use 'makeRollCallSelector' in react components
 * @param rollcallId The if of the rollcall / event to retrieve
 * @param state The redux state
 * @returns The constructed rollcall or undefined if the id is not found
 */
export const getRollCallById = (rollcallId: string, state: unknown) => {
  const rollcallById = getRollCallState(state).byId;

  if (!(rollcallId in rollcallById)) {
    return undefined;
  }

  return RollCall.fromState(rollcallById[rollcallId]);
};
