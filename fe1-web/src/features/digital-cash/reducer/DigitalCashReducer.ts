/**
 * This error is disabled since reducers use the createSlice function, which requires the user to
 * param-reassign. Please do not disable other errors.
 */
/* eslint-disable no-param-reassign */
import { createSelector, createSlice, PayloadAction } from '@reduxjs/toolkit';

import { Hash } from 'core/objects';
import { COINBASE_HASH } from 'resources/const';

import { TransactionState } from '../objects/transaction';

export interface DigitalCashReducerState {
  /**
   * A mapping between the public key hashes and their respective balances
   */
  balances: Record<string, number>;

  /**
   * Every hash of transactions received for this lao
   */
  allTransactionsHash: string[];

  /**
   * Transactions by their transaction hash (transaction id)
   */
  transactionsByHash: Record<string, TransactionState>;

  /**
   * A mapping between public key hashes and an array of hash of transactions which contains
   * this public key hash in one or more of their TxOuts
   */
  transactionsByPubHash: Record<string, string[]>;
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
        transactionState: TransactionState;
      }>,
    ) => {
      const { laoId, transactionState } = action.payload;

      if (!transactionState.transactionId) {
        throw new Error('The transaction id of the added transaction is not defined');
      }
      const transactionHash = transactionState.transactionId;

      /**
       * If state is empty for given lao or roll call, we should create the initial objects
       */
      if (!(laoId in state.byLaoId)) {
        state.byLaoId[laoId] = {
          balances: {},
          allTransactionsHash: [],
          transactionsByHash: {},
          transactionsByPubHash: {},
        };
      }

      const laoState: DigitalCashReducerState = state.byLaoId[laoId];

      laoState.transactionsByHash[transactionHash] = transactionState;
      laoState.allTransactionsHash.push(transactionHash);

      /**
       * Invariant for the digital cash implementation:
       * Every input of a public key used in an input will be spent in the outputs
       */
      transactionState.inputs.forEach((input) => {
        const pubHash = Hash.fromPublicKey(input.script.publicKey).valueOf();

        // If this is not a coinbase transaction, then as we are sure that all inputs are used
        if (input.txOutHash !== COINBASE_HASH) {
          laoState.balances[pubHash] = 0;
          laoState.transactionsByPubHash[pubHash] = [];
        }
      });

      transactionState.outputs.forEach((output) => {
        const pubKeyHash = output.script.publicKeyHash.valueOf();

        if (!laoState.balances[pubKeyHash]) {
          laoState.balances[pubKeyHash] = 0;
        }
        laoState.balances[pubKeyHash] += output.value;

        if (!(pubKeyHash in laoState.transactionsByPubHash)) {
          laoState.transactionsByPubHash[pubKeyHash] = [];
        }
        laoState.transactionsByPubHash[pubKeyHash].push(transactionHash);
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
 * Selector for the mapping between public key hashes and balances
 * @param laoId the lao in which to search for the balances mapping
 */
export const makeBalancesSelector = (laoId: Hash | string) =>
  createSelector(
    (state: any) => getDigitalCashState(state).byLaoId[laoId.valueOf()],
    (laoState: DigitalCashReducerState | undefined) => {
      return laoState?.balances || {};
    },
  );

/**
 * Selector for the balance of a user
 * @param laoId the lao in which to search for the balance
 * @param publicKey the public key that possesses this balance
 */
export const makeBalanceSelector = (laoId: Hash | string, publicKey: string) =>
  createSelector(
    (state: any) => getDigitalCashState(state).byLaoId[laoId.valueOf()],
    (laoState: DigitalCashReducerState | undefined) => {
      return laoState?.balances[Hash.fromPublicKey(publicKey).valueOf()] || 0;
    },
  );

/**
 * Selector for all transaction states by lao id
 * @param laoId
 */
export const makeTransactionsSelector = (laoId: Hash | string) =>
  createSelector(
    (state: any) => getDigitalCashState(state).byLaoId[laoId.valueOf()]?.allTransactionsHash,
    (state: any) => getDigitalCashState(state).byLaoId[laoId.valueOf()]?.transactionsByHash,
    (
      transactionHashes: string[] | undefined,
      transactionsByHash: Record<string, TransactionState> | undefined,
    ) => {
      if (transactionHashes && transactionsByHash) {
        return transactionHashes.map((hash) => transactionsByHash[hash]);
      }
      return [];
    },
  );

/**
 * Selector for the mapping between hashes and the transaction states by lao id
 * @param laoId
 */
export const makeTransactionsByHashSelector = (laoId: Hash | string) =>
  createSelector(
    (state: any) => getDigitalCashState(state).byLaoId[laoId.valueOf()]?.transactionsByHash,
    (transactionsByHash: Record<string, TransactionState> | undefined) => {
      return transactionsByHash || {};
    },
  );
