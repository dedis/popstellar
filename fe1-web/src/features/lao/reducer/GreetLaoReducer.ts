/**
 * This error is disabled since reducers use the createSlice function, which requires the user to
 * param-reassign. Please do not disable other errors.
 */
/* eslint-disable no-param-reassign */
import { createSlice, Draft, PayloadAction } from '@reduxjs/toolkit';

/**
 * Reducer & associated function implementation to store all ids of lao#greet messages per lao
 * This allows the LaoGreetWatcher to operate more efficiently
 */

export interface GreetLaoReducerState {
  byLaoId: {
    [laoId: string]: string[];
  };
}

const initialState: GreetLaoReducerState = {
  byLaoId: {},
};

export const GREET_LAO_REDUCER_PATH = 'greetLaoIds';

const greetLaoSlice = createSlice({
  name: GREET_LAO_REDUCER_PATH,
  initialState,
  reducers: {
    addGreetLaoMessage: (
      state: Draft<GreetLaoReducerState>,
      action: PayloadAction<{ laoId: string; messageId: string }>,
    ) => {
      const { laoId, messageId } = action.payload;
      if (laoId in state.byLaoId) {
        // prevent duplicate entries
        if (!state.byLaoId[laoId].includes(messageId)) {
          state.byLaoId[laoId].push(messageId);
        }
      } else {
        state.byLaoId[laoId] = [messageId];
      }
    },
  },
});

export const { addGreetLaoMessage } = greetLaoSlice.actions;

export const greetLaoReduce = greetLaoSlice.reducer;

export default {
  [GREET_LAO_REDUCER_PATH]: greetLaoSlice.reducer,
};

export const getGreetLaoState = (state: any): GreetLaoReducerState => state[GREET_LAO_REDUCER_PATH];

/**
 * A function to directly retrieve the list of all lao#greet message ids
 * @remark NOTE: This function does not memoize the result. If you need this, use makeServerSelector instead
 * @param state The redux state
 * @returns The list of message ids
 */
export const getAllGreetLaoMessageIds = (state: any): string[] => {
  const laoGreetState = getGreetLaoState(state);
  const laoIds = Object.keys(laoGreetState.byLaoId);

  return laoIds.flatMap((laoId) => laoGreetState.byLaoId[laoId]);
};
