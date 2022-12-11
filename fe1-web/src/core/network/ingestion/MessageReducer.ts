/**
 * This error is disabled since reducers use the createSlice function, which requires the user to
 * param-reassign. Please do not disable other errors.
 */
/* eslint-disable no-param-reassign */
import { createSelector, createSlice, PayloadAction } from '@reduxjs/toolkit';

import { Hash, WitnessSignatureState } from 'core/objects';

import { ExtendedMessage, ExtendedMessageState, markMessageAsProcessed } from './ExtendedMessage';

/**
 * Reducer & associated function implementation to store all known Messages
 */

export interface MessageReducerState {
  byId: Record<string, ExtendedMessageState>;
  allIds: string[];
  unprocessedIds: string[];
}

const initialState: MessageReducerState = {
  byId: {},
  allIds: [],
  unprocessedIds: [],
};

export const messageReducerPath = 'messages';

const messagesSlice = createSlice({
  name: messageReducerPath,
  initialState,
  reducers: {
    // Add a Message to the list of known Messages
    addMessages: {
      prepare(messages: ExtendedMessageState | ExtendedMessageState[]) {
        const msgs = Array.isArray(messages) ? messages : [messages];
        return { payload: msgs };
      },
      reducer(state, action: PayloadAction<ExtendedMessageState[]>) {
        const messages = action.payload;

        messages.forEach((msg: ExtendedMessageState) => {
          if (msg.message_id in state.byId) {
            // don't add again a message we have already received
            // TODO: we might want to merge the witness signatures here
            return;
          }

          state.byId[msg.message_id] = msg;
          state.allIds.push(msg.message_id);
          state.unprocessedIds.push(msg.message_id);
        });
      },
    },

    // Remove a Message to the list of unprocessed Messages
    processMessages: {
      prepare(messageIds: Hash | Hash[]) {
        const msgIds = Array.isArray(messageIds) ? messageIds : [messageIds];
        return {
          payload: msgIds.map((m: Hash) => m.valueOf()),
        };
      },
      reducer(state, action: PayloadAction<string[]>) {
        const messageIds = action.payload;

        messageIds.forEach((messageId: string) => {
          state.byId[messageId] = markMessageAsProcessed(state.byId[messageId]);
          state.unprocessedIds = state.unprocessedIds.filter((e) => e !== messageId);
        });
      },
    },

    // Empty the list of known Messages ("reset")
    clearAllMessages: (state) => {
      state.byId = {};
      state.allIds = [];
      state.unprocessedIds = [];
    },

    // Add witness signatures to a message
    addMessageWitnessSignature: {
      prepare(messageId: Hash | string, witSig: WitnessSignatureState) {
        return {
          payload: {
            messageId: messageId.valueOf(),
            witnessSignature: witSig,
          },
        };
      },
      reducer(
        state,
        action: PayloadAction<{
          messageId: string;
          witnessSignature: WitnessSignatureState;
        }>,
      ) {
        const { messageId, witnessSignature } = action.payload;

        if (!(messageId in state.byId)) {
          return;
        }

        const msg = state.byId[messageId];
        if (!msg.witness_signatures.every((ws) => ws.witness !== witnessSignature.witness)) {
          // avoid adding multiple times the same witness signature
          return;
        }

        msg.witness_signatures.push(witnessSignature);
      },
    },
  },
});

export const { addMessages, processMessages, addMessageWitnessSignature, clearAllMessages } =
  messagesSlice.actions;

export function getMessagesState(state: any): MessageReducerState {
  return state[messageReducerPath];
}

export function getMessage(
  state: MessageReducerState,
  messageId: Hash,
): ExtendedMessage | undefined {
  const id = messageId.valueOf();
  return id in state.byId ? ExtendedMessage.fromState(state.byId[id]) : undefined;
}

/**
 * Creates a redux-toolkit selector that memoizes the result if the input do not change
 * Intended for the use in combination with useSelector()
 * @param messageId The id of the message
 * @returns A redux selector
 */
export const makeMessageSelector = (messageId: Hash) =>
  createSelector(
    // First input: map of message ids to messages
    (state: any) => getMessagesState(state).byId,
    (byId: Record<string, ExtendedMessageState>): ExtendedMessage | undefined => {
      const serializedMessageId = messageId.valueOf();

      if (serializedMessageId in byId) {
        return ExtendedMessage.fromState(byId[serializedMessageId]);
      }

      return undefined;
    },
  );

export const messageReduce = messagesSlice.reducer;

export default {
  [messageReducerPath]: messagesSlice.reducer,
};
