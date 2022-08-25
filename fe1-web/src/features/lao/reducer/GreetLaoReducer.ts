/**
 * This error is disabled since reducers use the createSlice function, which requires the user to
 * param-reassign. Please do not disable other errors.
 */
/* eslint-disable no-param-reassign */
import { createSelector, createSlice, Draft, PayloadAction } from '@reduxjs/toolkit';

import { getMessagesState } from 'core/network/ingestion';
import { ExtendedMessageState } from 'core/network/ingestion/ExtendedMessage';
import { WitnessSignatureState } from 'core/objects';

/**
 * Reducer & associated function implementation to store all ids of *unhandled* lao#greet messages
 * This allows the LaoGreetWatcher to operate more efficiently
 */

export interface GreetLaoReducerState {
  unhandledIds: string[];
}

const initialState: GreetLaoReducerState = {
  unhandledIds: [],
};

export const GREET_LAO_REDUCER_PATH = 'greetLaoIds';

const greetLaoSlice = createSlice({
  name: GREET_LAO_REDUCER_PATH,
  initialState,
  reducers: {
    addUnhandledGreetLaoMessage: (
      state: Draft<GreetLaoReducerState>,
      action: PayloadAction<{ messageId: string }>,
    ) => {
      const { messageId } = action.payload;

      // prevent duplicate entries
      if (!state.unhandledIds.includes(messageId)) {
        state.unhandledIds.push(messageId);
      }
    },
    /** Removes a message from the GreetLao unhandledIds */
    handleGreetLaoMessage: (
      state: Draft<GreetLaoReducerState>,
      action: PayloadAction<{ messageId: string }>,
    ) => {
      const { messageId } = action.payload;

      state.unhandledIds = state.unhandledIds.filter((id) => id !== messageId);
    },
  },
});

export const { addUnhandledGreetLaoMessage, handleGreetLaoMessage } = greetLaoSlice.actions;

export const greetLaoReduce = greetLaoSlice.reducer;

export default {
  [GREET_LAO_REDUCER_PATH]: greetLaoSlice.reducer,
};

export const getGreetLaoState = (state: any): GreetLaoReducerState => state[GREET_LAO_REDUCER_PATH];

/**
 * A function to directly retrieve the list of all unhandled lao#greet message ids
 * @remark This function does not memoize the result since it just returns part of the store
 * @param state The redux state
 * @returns The list of message ids
 */
export const getUnhandledGreetLaoMessageIds = (state: any): string[] =>
  getGreetLaoState(state)?.unhandledIds || [];

const sGetUnhandledGreetLaoMessageIds = (state: any) => getUnhandledGreetLaoMessageIds(state);
const sGetMessagesById = (state: any) => getMessagesState(state)?.byId;

/**
 * A selector returning a map from message ids to witness signatures
 * for all message ids of lao#greet messages that have not been handled yet.
 */
export const selectUnhandledGreetLaoWitnessSignaturesByMessageId = createSelector(
  // First input: all unhandled lao greet message ids
  sGetUnhandledGreetLaoMessageIds,
  // Second input: the map of messageIds to the message state
  sGetMessagesById || {},
  // Selector: returns a map from lao#greet message id to witness signature
  (
    messageIds: string[],
    byId: Record<string, ExtendedMessageState>,
  ): { [messageId: string]: WitnessSignatureState[] } => {
    const signaturesByMessageId: { [messageId: string]: WitnessSignatureState[] } = {};

    for (const messageId of messageIds) {
      signaturesByMessageId[messageId] = byId[messageId].witness_signatures;
    }

    return signaturesByMessageId;
  },
);
