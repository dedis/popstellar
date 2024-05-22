/**
 * This error is disabled since reducers use the createSlice function, which requires the user to
 * param-reassign. Please do not disable other errors.
 */
/* eslint-disable no-param-reassign */
import { createSelector, createSlice, PayloadAction } from '@reduxjs/toolkit';

import { Hash } from 'core/objects';

import { LinkedOrganizationState } from '../objects/LinkedOrganization';

export const LINKEDORGANIZATIONS_REDUCER_PATH = 'linked_organizations';

export interface LinkedOrganizationReducerState {
  byLaoId: {
    [laoId: string]: {
      byLinkedLaoId: Record<string, LinkedOrganizationState>;
      allLaoIds: string[];
      allLaos: LinkedOrganizationState[];
    };
  };
}

const initialState: LinkedOrganizationReducerState = {
  byLaoId: {},
};

const linkedOrganizationSlice = createSlice({
  name: LINKEDORGANIZATIONS_REDUCER_PATH,
  initialState,
  reducers: {
    // Add a Organization to the list of Linked Organizations
    addLinkedOrganization: {
      prepare(laoId: Hash, linkedOrganization: LinkedOrganizationState) {
        return {
          payload: {
            laoId: laoId.valueOf(),
            linkedOrganization: linkedOrganization,
          },
        };
      },
      reducer(state, action: PayloadAction<{ laoId: string; linkedOrganization: LinkedOrganizationState }>) {
        const { laoId, linkedOrganization } = action.payload;

        if (state.byLaoId[laoId] === undefined) {
          state.byLaoId[laoId] = {
            allLaoIds: [],
            byLinkedLaoId: {},
            allLaos: [],
          };
        }

        if (state.byLaoId[laoId].allLaoIds.includes(linkedOrganization.lao_id.valueOf())) {
          throw new Error(
            `Tried to store organization with lao id ${linkedOrganization.lao_id} but there already exists one with the same lao id`,
          );
        }
        console.log(state.byLaoId[laoId].allLaoIds);
        console.log(state.byLaoId[laoId].allLaos);

        state.byLaoId[laoId].allLaoIds.push(linkedOrganization.lao_id);
        state.byLaoId[laoId].allLaos.push(linkedOrganization);
        state.byLaoId[laoId].byLinkedLaoId[linkedOrganization.lao_id] = linkedOrganization;
      },
    },
  },
});

export const { addLinkedOrganization: addLinkedOrganization } = linkedOrganizationSlice.actions;

export const getLinkedOrganizationState = (state: any): LinkedOrganizationReducerState =>
  state[LINKEDORGANIZATIONS_REDUCER_PATH];

/**
 * Retrives a single linked organization state by id
 * @param laoId The id of the lao
 * @param linked_lao_id The lao id of the linked organization to retrieve
 * @returns A linked organization state
 */
export const makeSingleLinkedOrganizationSelector = (laoId: Hash, linked_lao_id: string) => {
  return createSelector(
    // First input: a map containing all linked organizations
    (state: any) => getLinkedOrganizationState(state),
    // Selector: returns the linked organization for a specific lao and linked_lao_id
    (linkedOrganizationState: LinkedOrganizationReducerState): LinkedOrganizationState | undefined => {
      const serializedLaoId = laoId.valueOf();
      return linkedOrganizationState.byLaoId[serializedLaoId]?.byLinkedLaoId[linked_lao_id];
    },
  );
};

/**
 * Retrives all linked organization state by lao id
 * @param laoId The id of the lao
 * @returns A list of linked organization state
 */
export const makeLinkedOrganizationSelector = (laoId: Hash) => {
  return createSelector(
    // First input: a map containing all linked organizations
    (state: any) => getLinkedOrganizationState(state),
    // Selector: returns the linked organization for a specific lao and linked_lao_id
    (linkedOrganizationState: LinkedOrganizationReducerState): LinkedOrganizationState[] | [] => {
      const serializedLaoId = laoId.valueOf();
      if (!linkedOrganizationState.byLaoId[serializedLaoId]) {
        return [];
      }
      return linkedOrganizationState.byLaoId[serializedLaoId].allLaos;
    },
  );
};

export const linkedOrganizationsReduce = linkedOrganizationSlice.reducer;

export default {
  [LINKEDORGANIZATIONS_REDUCER_PATH]: linkedOrganizationSlice.reducer,
};
