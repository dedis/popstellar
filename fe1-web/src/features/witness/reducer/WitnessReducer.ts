/**
 * This error is disabled since reducers use the createSlice function, which requires the user to
 * param-reassign. Please do not disable other errors.
 */
/* eslint-disable no-param-reassign */
import { createSlice, Draft, PayloadAction } from '@reduxjs/toolkit';

export const WITNESS_REDUCER_PATH = 'witness';

export interface MessagesToWitnessReducerState {
  allIds: string[];
}

const initialState: MessagesToWitnessReducerState = {
  allIds: [],
};

const messagesToWitnessSlice = createSlice({
  name: WITNESS_REDUCER_PATH,
  initialState,
  reducers: {
    // Action called when a message has been witnessed
    addMessageToWitness: (
      state: Draft<MessagesToWitnessReducerState>,
      action: PayloadAction<{ messageId: string }>,
    ) => {
      const { messageId } = action.payload;

      if (!state.allIds.includes(messageId)) {
        state.allIds.push(messageId);
      }
    },

    removeMessageToWitness: (
      state: Draft<MessagesToWitnessReducerState>,
      action: PayloadAction<string>,
    ) => {
      const messageId = action.payload;

      if (!state.allIds.includes(messageId)) {
        console.warn(
          `Tried to remove the message to witness with id ${messageId} but this message id has never been stored`,
        );
        return;
      }

      state.allIds = state.allIds.filter((id) => id !== messageId);
    },
  },
});

export const { addMessageToWitness, removeMessageToWitness } = messagesToWitnessSlice.actions;

export const getMessagesToWitnessState = (state: any): MessagesToWitnessReducerState =>
  state[WITNESS_REDUCER_PATH];

/**
 * Checks whether a given message id is stored here / is to be witnessed
 * MUST NOT BE USED IN useSelector().
 * @param messageId The id of the message to retrieve
 * @param state The redux state
 */
export const isMessageToWitness = (messageId: string, state: unknown): boolean => {
  const { allIds } = getMessagesToWitnessState(state);

  return allIds.includes(messageId);
};

export const witnessReduce = messagesToWitnessSlice.reducer;

export default {
  [WITNESS_REDUCER_PATH]: messagesToWitnessSlice.reducer,
};
