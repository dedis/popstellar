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
      byLaoId: Record<string, LinkedOrganizationState>;
      allLaoIds: string[];
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
    addOrganization: {
      prepare(laoId: Hash, organization: LinkedOrganizationState) {
        return {
          payload: {
            laoId: laoId.valueOf(),
            organization,
          },
        };
      },
      reducer(state, action: PayloadAction<{ laoId: string; organization: LinkedOrganizationState }>) {
        const { laoId, organization } = action.payload;

        if (state.byLaoId[laoId] === undefined) {
          state.byLaoId[laoId] = {
            allLaoIds: [],
            byLaoId: {},
          };
        }

        if (state.byLaoId[laoId].allLaoIds.includes(organization.lao_id.valueOf())) {
          throw new Error(
            `Tried to store organization with lao id ${organization.lao_id} but there already exists one with the same lao id`,
          );
        }

        state.byLaoId[laoId].allLaoIds.push(organization.lao_id);
        state.byLaoId[laoId].byLaoId[organization.lao_id] = organization;
      },
    },
  },
});

export const { addOrganization } = linkedOrganizationSlice.actions;

export const getLinkedOrganizationState = (state: any): LinkedOrganizationReducerState =>
  state[LINKEDORGANIZATIONS_REDUCER_PATH];

/**
 * Retrives a linked organization state by id
 * @param laoId The id of the lao
 * @param linked_lao_id The lao id of the linked organization to retrieve
 * @returns A linked organization state
 */
export const makeLinkedOrganizationSelector = (laoId: Hash, linked_lao_id: string) => {
  return createSelector(
    // First input: a map containing all linked organizations
    (state: any) => getLinkedOrganizationState(state),
    // Selector: returns the linked organization for a specific lao and linked_lao_id
    (linkedOrganizationState: LinkedOrganizationReducerState): LinkedOrganizationState | undefined => {
      const serializedLaoId = laoId.valueOf();
      return linkedOrganizationState.byLaoId[serializedLaoId]?.byLaoId[linked_lao_id];
    },
  );
};

export const linkedOrganizationsReduce = linkedOrganizationSlice.reducer;

export default {
  [LINKEDORGANIZATIONS_REDUCER_PATH]: linkedOrganizationSlice.reducer,
};
