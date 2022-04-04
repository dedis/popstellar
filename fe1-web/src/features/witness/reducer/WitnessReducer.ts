/**
 * This error is disabled since reducers use the createSlice function, which requires the user to
 * param-reassign. Please do not disable other errors.
 */
/* eslint-disable no-param-reassign */
import { createSelector, createSlice, Draft, PayloadAction } from '@reduxjs/toolkit';

import { ExtendedMessage, ExtendedMessageState } from 'core/network/ingestion/ExtendedMessage';

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

      state.byId[message.message_id] = message;
      state.allIds.push(message.message_id);
    },

    // Add a message that has to be witnessed
    removeMessageToWitness: (
      state: Draft<MessagesToWitnessReducerState>,
      action: PayloadAction<string>,
    ) => {
      const messageId = action.payload;

      if (!(messageId in state.byId)) {
        // this message was never stored?
        return;
      }

      delete state.byId[messageId];
      state.allIds = state.allIds.filter((id) => id !== messageId);
    },
  },
});

export const { addMessageToWitness, removeMessageToWitness } = messagesToWitnessSlice.actions;

export const getMessagesToWitnessState = (state: any): MessagesToWitnessReducerState =>
  state[WITNESS_REDUCER_PATH];

export const makeMessageSelector = (messageId: string) =>
  createSelector(
    // First input: map of message ids to messages
    (state) => getMessagesToWitnessState(state).byId,
    (byId: Record<string, ExtendedMessageState>): ExtendedMessage | undefined => {
      if (messageId in byId) {
        return ExtendedMessage.fromState(byId[messageId]);
      }

      return undefined;
    },
  );

export const witnessReduce = messagesToWitnessSlice.reducer;

export default {
  [WITNESS_REDUCER_PATH]: messagesToWitnessSlice.reducer,
};
