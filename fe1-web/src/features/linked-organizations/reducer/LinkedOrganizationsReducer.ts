/**
 * This error is disabled since reducers use the createSlice function, which requires the user to
 * param-reassign. Please do not disable other errors.
 */
/* eslint-disable no-param-reassign */
import { PayloadAction, createSelector, createSlice } from '@reduxjs/toolkit';

import { Hash, PublicKey, RollCallToken } from 'core/objects';

export interface LinkedOrganizationsReducerState {
  
}

export interface LinkedOrganizationsLaoReducerState {
  byLaoId: Record<string, LinkedOrganizationsReducerState>;
}

/* Initial state of the digital cash transactions */
const initialState: LinkedOrganizationsLaoReducerState = {
  byLaoId: {},
};

/* Name of digital cash slice in storage */
export const LINKED_ORGANIZATIONS_REDUCER_PATH = 'linkedOrganizations';


const linkedOrganizationsSlice = createSlice({
  name: LINKED_ORGANIZATIONS_REDUCER_PATH,
  initialState,
  reducers: {
    /*
     * Adds a transaction to the state
     * We are trusting information of the transaction object, we do not verify it
     */
    addTransaction: {
      prepare: (laoId: Hash) => ({
        payload: { laoId: laoId.toState()},
      }),
      reducer: (
        state,
        action: PayloadAction<{
          laoId: string;
        }>,
      ) => {
        const { laoId } = action.payload;

        /**
         * If state is empty for given lao or roll call, we should create the initial objects
         */
        if (!(laoId in state.byLaoId)) {
          state.byLaoId[laoId] = {
          };
        }

        const laoState: LinkedOrganizationsReducerState = state.byLaoId[laoId];
      },
    },
  },
});

export const linkedOrganizationsReducer = linkedOrganizationsSlice.reducer;

export default {
  [LINKED_ORGANIZATIONS_REDUCER_PATH]: linkedOrganizationsSlice.reducer,
};

export const getLinkedOrganizationsState = (state: any): LinkedOrganizationsReducerState =>
  state[LINKED_ORGANIZATIONS_REDUCER_PATH];

