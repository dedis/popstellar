/**
 * This error is disabled since reducers use the createSlice function, which requires the user to
 * param-reassign. Please do not disable other errors.
 */
/* eslint-disable no-param-reassign */
import { createSelector, createSlice, Draft, PayloadAction } from '@reduxjs/toolkit';

import { Hash, PublicKey } from 'core/objects';
import { isDefined } from 'core/types';

import { RollCall, RollCallState } from '../objects';

/**
 * Reducer & associated function implementation to store all known rollcalls
 */

export interface RollCallReducerState {
  byId: Record<string, RollCallState>;
  allIds: string[];

  /**
   * idAlias stores the ID aliases.
   * @remarks
   * If a new message (with a new_id) changes the state of an event (with old_id),
   * this map associates new_id -> old_id.
   * This ensures that we can keep only one event in memory, with its up-to-date state,
   * but future messages can refer to new_id as needed.
   */
  idAlias: Record<string, string>;
}

const initialState: RollCallReducerState = {
  byId: {},
  allIds: [],
  idAlias: {},
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
      if (newRollCall.idAlias) {
        state.idAlias[newRollCall.idAlias] = newRollCall.id;
      }
    },

    updateRollCall: (state: Draft<RollCallReducerState>, action: PayloadAction<RollCallState>) => {
      const updatedRollCall = action.payload;

      if (!(updatedRollCall.id in state.byId)) {
        throw new Error(`Tried to update inexistent rollcall with id ${updatedRollCall.id}`);
      }

      const oldAlias = state.byId[updatedRollCall.id].idAlias;
      if (oldAlias) {
        delete state.idAlias[oldAlias];
      }

      state.byId[updatedRollCall.id] = updatedRollCall;
      if (updatedRollCall.idAlias) {
        state.idAlias[updatedRollCall.idAlias] = updatedRollCall.id;
      }
    },

    removeRollCall: (state, action: PayloadAction<Hash | string>) => {
      const rollcallId = action.payload.valueOf();

      if (!(rollcallId in state.byId)) {
        throw new Error(`Tried to delete inexistent rollcall with id ${rollcallId}`);
      }

      const alias = state.byId[rollcallId].idAlias;
      if (alias) {
        delete state.idAlias[alias];
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
 * @param rollCallId The if of the rollcall / event to retrieve
 * @returns The selector
 */
export const makeRollCallSelector = (rollCallId: Hash | string | undefined) => {
  const rollCallIdString = rollCallId?.valueOf() || 'undefined';

  return createSelector(
    // First input: map from ids to rollcalls
    (state: any) => getRollCallState(state).byId,
    // Second input: Alias for the given event id
    (state: any) => getRollCallState(state).idAlias[rollCallIdString],
    // Selector: returns the selected rollcall
    (
      rollcallById: Record<string, RollCallState>,
      idAlias: string | undefined,
    ): RollCall | undefined => {
      if (idAlias) {
        if (!(idAlias in rollcallById)) {
          throw new Error(
            `Found alias mapping ${rollCallIdString} to ${idAlias} but no roll call with id ${idAlias} is stored`,
          );
        }

        return RollCall.fromState(rollcallById[idAlias]);
      }

      if (!rollCallIdString || !(rollCallIdString in rollcallById)) {
        return undefined;
      }

      return RollCall.fromState(rollcallById[rollCallIdString]);
    },
  );
};

/**
 * Creates a selector that retrieves a map from rollcall id to roll calls for a list of given roll call ids
 * @param rollCallIds The id of the rollcalls to retrieve
 * @returns The selector
 */
export const makeRollCallByIdSelector = (rollCallIds: string[]) =>
  createSelector(
    // First input: map from ids to rollcalls
    (state: any) => getRollCallState(state).byId,
    // Selector: returns the selected rollcall
    (rollCallById: Record<string, RollCallState>): Record<string, RollCall> => {
      return (
        rollCallIds
          // create a roll call instance for all given ids
          .map((id) => (id in rollCallById ? RollCall.fromState(rollCallById[id]) : undefined))
          // filter out undefined values
          .filter(isDefined)
          // create a map from it
          .reduce<Record<string, RollCall>>((map, rollCall) => {
            map[rollCall.id.valueOf()] = rollCall;
            return map;
          }, {})
      );
    },
  );

/**
 * Retrieves a rollcall by its id from the redux store
 * @remark This function does not memoize its result, use 'makeRollCallSelector' in react components
 * @param rollCallId The id of the rollcall / event to retrieve
 * @param state The redux state
 * @returns The constructed rollcall or undefined if the id is not found
 */
export const getRollCallById = (rollCallId: Hash | string, state: unknown) => {
  const rollCallIdString = rollCallId.valueOf();

  const rollcallById = getRollCallState(state).byId;
  const idAlias = getRollCallState(state).idAlias[rollCallIdString];

  if (idAlias) {
    if (!(idAlias in rollcallById)) {
      throw new Error(
        `Found alias mapping ${rollCallIdString} to ${idAlias} but no roll call with id ${idAlias} is stored`,
      );
    }

    return RollCall.fromState(rollcallById[idAlias]);
  }

  if (!(rollCallIdString in rollcallById)) {
    return undefined;
  }

  return RollCall.fromState(rollcallById[rollCallIdString]);
};

/**
 * Returns the list of attendees of a roll call.
 *
 * @param rollCallId - The id of the roll call
 */
export const makeRollCallAttendeesListSelector = (rollCallId: Hash | string | undefined) => {
  const rollCallIdString = rollCallId?.valueOf();

  return createSelector(
    // First input: Get all events across all LAOs
    (state: any) => getRollCallState(state),
    // Selector: returns a map of ids -> LaoEvents
    (eventMap: RollCallReducerState): PublicKey[] => {
      if (!rollCallIdString || !(rollCallIdString in eventMap.byId)) {
        return [];
      }

      const rollCall = RollCall.fromState(eventMap.byId[rollCallIdString]);

      return rollCall.attendees || [];
    },
  );
};
