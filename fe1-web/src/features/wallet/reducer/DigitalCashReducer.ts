/**
 * This error is disabled since reducers use the createSlice function, which requires the user to
 * param-reassign. Please do not disable other errors.
 */
/* eslint-disable no-param-reassign */
import { createSelector, createSlice, PayloadAction } from '@reduxjs/toolkit';

import { Hash } from 'core/objects';

import STRINGS from '../../../resources/strings';
import { TransactionState } from '../objects/transaction';

export interface DigitalCashReducerState {
  /**
   * A mapping between the public key hashes and their respective balances
   */
  balances: Record<string, number>;

  /**
   * Every transaction received for this roll call
   */
  transactions: TransactionState[];

  /**
   * Transactions by their transaction hash (transaction id)
   */
  transactionsByHash: Record<string, TransactionState>;

  /**
   * A mapping between public key hashes and a set of the transactions which contain this hash
   * in one or more of their TxOuts
   */
  transactionsByPubHash: Record<string, TransactionState[]>;
}
export interface DigitalCashLaoReducerState {
  byLaoId: Record<string, DigitalCashReducerState>;
}

/* Initial state of the digital cash transactions */
const initialState: DigitalCashLaoReducerState = {
  byLaoId: {},
};

/* Name of digital cash slice in storage */
export const DIGITAL_CASH_REDUCER_PATH = 'digitalCash';

const digitalCashSlice = createSlice({
  name: DIGITAL_CASH_REDUCER_PATH,
  initialState,
  reducers: {
    /*
     * Adds a transaction to the state
     * We are trusting information of the transaction object, we do not verify it
     */
    addTransaction: (
      state,
      action: PayloadAction<{
        laoId: string;
        transactionMessage: TransactionState;
      }>,
    ) => {
      const { laoId, transactionMessage } = action.payload;

      /**
       * If state is empty for given lao or roll call, we should create the initial objects
       */
      if (!(laoId in state.byLaoId)) {
        state.byLaoId[laoId] = {
          balances: {},
          transactions: [],
          transactionsByHash: {},
          transactionsByPubHash: {},
        };
      }

      const laoState: DigitalCashReducerState = state.byLaoId[laoId];

      laoState.transactionsByHash[transactionMessage.transactionId!] = transactionMessage;
      laoState.transactions.push(transactionMessage);

      /**
       * Invariant for the digital cash implementation:
       * Every input of a public key used in an input will be spent in the outputs
       */
      transactionMessage.inputs.forEach((input) => {
        const pubHash = Hash.fromPublicKey(input.script.publicKey).valueOf();
        // If this is not a coinbase transaction, then as we are sure that all inputs are used
        if (input.txOutHash !== STRINGS.coinbase_hash) {
          laoState.balances[pubHash] = 0;
          laoState.transactionsByPubHash[pubHash] = [];
        }
      });

      transactionMessage.outputs.forEach((output) => {
        const pubKeyHash = output.script.publicKeyHash.valueOf();
        if (!laoState.balances[pubKeyHash]) {
          laoState.balances[pubKeyHash] = 0;
        }
        laoState.balances[pubKeyHash] += output.value;
        if (!(pubKeyHash in laoState.transactionsByPubHash)) {
          laoState.transactionsByPubHash[pubKeyHash] = [];
        }
        console.log(`State contains ${laoState.transactionsByPubHash[pubKeyHash]}`);
        laoState.transactionsByPubHash[pubKeyHash].push(transactionMessage);
      });
    },
  },
});
export const { addTransaction } = digitalCashSlice.actions;
export const digitalCashReduce = digitalCashSlice.reducer;

export default {
  [DIGITAL_CASH_REDUCER_PATH]: digitalCashSlice.reducer,
};

export const getDigitalCashState = (state: any): DigitalCashLaoReducerState =>
  state[DIGITAL_CASH_REDUCER_PATH];

/**
 * Balance selector
 * @param laoId the lao in which to search for the balance
 * @param publicKey the public key that possesses this balance
 */
export const makeBalanceSelector = (laoId: Hash, publicKey: string) =>
  createSelector(
    (state) => getDigitalCashState(state).byLaoId[laoId.valueOf()],
    (laoState: DigitalCashReducerState | undefined) => {
      return laoState?.balances[Hash.fromPublicKey(publicKey).valueOf()] || 0;
    },
  );
