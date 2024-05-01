/**
 * This error is disabled since reducers use the createSlice function, which requires the user to
 * param-reassign. Please do not disable other errors.
 */
/* eslint-disable no-param-reassign */
import { createSelector, createSlice, PayloadAction } from '@reduxjs/toolkit';

import { Hash } from 'core/objects';

import { OrganizationState } from '../objects/Organization';

export const LINKEDORGANIZATIONS_REDUCER_PATH = 'linked_organizations';

export interface OrganizationReducerState {
  byLaoId: {
    [laoId: string]: {
      byLaoId: Record<string, OrganizationState>;
      allLaoIds: string[];
    };
  };
}

const initialState: OrganizationReducerState = {
  byLaoId: {},
};

const organizationSlice = createSlice({
  name: LINKEDORGANIZATIONS_REDUCER_PATH,
  initialState,
  reducers: {
    // Add a Organization to the list of Linked Organizations
    addOrganization: {
      prepare(laoId: Hash, organization: OrganizationState) {
        return {
          payload: {
            laoId: laoId.valueOf(),
            organization,
          },
        };
      },
      reducer(state, action: PayloadAction<{ laoId: string; organization: OrganizationState }>) {
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

export const { addOrganization } = organizationSlice.actions;

export const getOrganizationState = (state: any): OrganizationReducerState =>
  state[LINKEDORGANIZATIONS_REDUCER_PATH];

/**
 * Retrives a single notification state by id
 * @param laoId The id of the lao
 * @param notificationId The id of the notification to retrieve
 * @returns A single notification state
 */
export const makeLinkedOrganizationSelector = (laoId: Hash, linked_lao_id: string) => {
  return createSelector(
    // First input: a map containing all notifications
    (state: any) => getOrganizationState(state),
    // Selector: returns the notification for a specific lao and notification id
    (organizationState: OrganizationReducerState): OrganizationState | undefined => {
      const serializedLaoId = laoId.valueOf();
      return organizationState.byLaoId[serializedLaoId]?.byLaoId[linked_lao_id];
    },
  );
};

export const linkedOrganizationsReduce = organizationSlice.reducer;

export default {
  [LINKEDORGANIZATIONS_REDUCER_PATH]: organizationSlice.reducer,
};
