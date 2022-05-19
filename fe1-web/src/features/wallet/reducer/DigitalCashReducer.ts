/**
 * This error is disabled since reducers use the createSlice function, which requires the user to
 * param-reassign. Please do not disable other errors.
 */
/* eslint-disable no-param-reassign */
import { createSlice, PayloadAction } from '@reduxjs/toolkit';

import { Hash } from 'core/objects';

import { TransactionState } from '../objects/transaction';

interface DigitalCashReducerState {
  /**
   * A mapping between the public key hashes and their respective balances
   */
  balances: Record<string, number>;

  transactions: TransactionState[];
  transactionsByHash: Record<string, TransactionState>;
  /**
   * A mapping between public key hashes and a set of the transactions which contain this hash
   * in one or more of their TxOuts
   */
  transactionsByPubHash: Record<string, Set<TransactionState>>;
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
     * We are trusting information of the transaction object, we do not verify it
     */
    addTransaction: {
      prepare(
        laoId: Hash | string,
        rollCallId: Hash | string,
        transactionMessage: TransactionState,
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
          transactionMessage: TransactionState;
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
            balances: {},
            transactions: [],
            transactionsByHash: {},
            transactionsByPubHash: {},
          };
        }

        const rollCallState: DigitalCashReducerState = state.byLaoId[laoId].byRCId[rollCallId];

        rollCallState.transactionsByHash[transactionMessage.transactionId!] = transactionMessage;
        rollCallState.transactions.push(transactionMessage);

        // Invariant: Every inputs of a public key used in an input will be spent in the outputs
        transactionMessage.inputs.forEach((input) => {
          const pubHash = Hash.fromString(input.script.publicKey).valueOf();
          rollCallState.balances[pubHash] = 0;
          rollCallState.transactionsByPubHash[pubHash].clear();
        });

        transactionMessage.outputs.forEach((output) => {
          rollCallState.balances[output.script.publicKeyHash] += output.value;
          rollCallState.transactionsByPubHash[output.script.publicKeyHash].add(transactionMessage);
        });
      },
    },
  },
});
export const { addTransaction } = digitalCashSlice.actions;
export const digitalCashReduce = digitalCashSlice.reducer;

export default {
  [DIGITAL_CASH_REDUCER_PATH]: digitalCashSlice.reducer,
};

export const getDigitalCashState = (state: any): DigitalCashReducerState =>
  state[DIGITAL_CASH_REDUCER_PATH];
