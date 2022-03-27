/**
 * This error is disabled since reducers use the createSlice function, which requires the user to
 * param-reassign. Please do not disable other errors.
 */
/* eslint-disable no-param-reassign */
import { createSlice, Draft, PayloadAction } from '@reduxjs/toolkit';

import { ExtendedMessageState } from 'core/network/ingestion/ExtendedMessage';

export const WITNESS_REDUCER_PATH = 'witness';

export interface MessagesToWitnessReducerState {
  byId: Record<string, ExtendedMessageState>;
  allIds: string[];
}

const initialState: MessagesToWitnessReducerState = {
  byId: {},
  allIds: [],
};

const messagesToWitnessSlice = createSlice({
  name: WITNESS_REDUCER_PATH,
  initialState,
  reducers: {
    // Action called when a message has been witnessed
    addMessageToWitness: (
      state: Draft<MessagesToWitnessReducerState>,
      action: PayloadAction<ExtendedMessageState>,
    ) => {
      const message = action.payload;

      if (message.message_id in state.byId) {
        // this message is already stored in the reducer
        return;
      }

      state.byId[message.message_id.valueOf()] = message;
      state.allIds.push(message.message_id.valueOf());
    },

    // Add a message that has to be witnessed
    witnessMessage: (
      state: Draft<MessagesToWitnessReducerState>,
      action: PayloadAction<ExtendedMessageState>,
    ) => {
      const message = action.payload;

      if (!(message.message_id.valueOf() in state.byId)) {
        // this message was never stored?
        return;
      }

      delete state.byId[message.message_id.valueOf()];
      state.allIds = state.allIds.filter((id) => id !== message.message_id.valueOf());
    },
  },
});

export const { addMessageToWitness, witnessMessage } = messagesToWitnessSlice.actions;

export const getMessagesToWitnessState = (state: any): MessagesToWitnessReducerState =>
  state[WITNESS_REDUCER_PATH];

export const witnessReduce = messagesToWitnessSlice.reducer;

export default {
  [WITNESS_REDUCER_PATH]: messagesToWitnessSlice.reducer,
};
