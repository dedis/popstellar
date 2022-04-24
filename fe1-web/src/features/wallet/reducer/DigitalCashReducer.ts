/**
 * This error is disabled since reducers use the createSlice function, which requires the user to
 * param-reassign. Please do not disable other errors.
 */
/* eslint-disable no-param-reassign */
import { createSlice, PayloadAction } from '@reduxjs/toolkit';

import { Hash } from 'core/objects';

import { DigitalCashMessage, DigitalCashTransaction } from '../network/DigitalCashTransaction';

interface DigitalCashReducerState {
  transactionMessages: DigitalCashMessage[];
  transactionsByHash: Record<string, DigitalCashTransaction>;
}
interface DigitalCashRollCallReducerState {
  byRCId: Record<string, DigitalCashReducerState>;
}
interface DigitalCashLaoReducerState {
  byLaoId: Record<string, DigitalCashRollCallReducerState>;
}

/* Initial state of the digital cash transactions */
const initialState: DigitalCashLaoReducerState = {
  byLaoId: {},
};

/* Name of digital cash slice in storage */
const DIGITAL_CASH_REDUCER_PATH = 'digitalCash';

const digitalCashSlice = createSlice({
  name: DIGITAL_CASH_REDUCER_PATH,
  initialState,
  reducers: {
    /*
     * Adds a transaction to the state
     * We are trusting information of the transaction object, we do not verify any hashes
     */
    addTransaction: {
      prepare(
        laoId: Hash | string,
        rollCallId: Hash | string,
        transactionMessage: DigitalCashMessage,
      ): any {
        return {
          payload: {
            laoId: laoId.valueOf(),
            rollCallId: rollCallId.valueOf(),
            transactionMessage: transactionMessage,
          },
        };
      },
      reducer(
        state,
        action: PayloadAction<{
          laoId: string;
          rollCallId: string;
          transactionMessage: DigitalCashMessage;
        }>,
      ) {
        const { laoId, rollCallId, transactionMessage } = action.payload;

        if (!(laoId in state.byLaoId)) {
          state.byLaoId[laoId] = {
            byRCId: {},
          };
        }

        if (!(rollCallId in state.byLaoId[laoId])) {
          state.byLaoId[laoId].byRCId[rollCallId] = {
            transactionMessages: [],
            transactionsByHash: {},
          };
        }

        const rollCallState: DigitalCashReducerState = state.byLaoId[laoId].byRCId[rollCallId];
        rollCallState.transactionsByHash[transactionMessage.transactionID.valueOf()] =
          transactionMessage.transaction;
        rollCallState.transactionMessages.push(transactionMessage);
      },
    },
  },
});
