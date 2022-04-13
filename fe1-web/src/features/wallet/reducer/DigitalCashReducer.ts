import { createSlice, PayloadAction } from '@reduxjs/toolkit';

import { Hash } from 'core/objects';

import { DigitalCashTransaction, TxOut } from '../network/DigitalCashTransaction';

interface DigitalCashReducerState {
  transactions: DigitalCashTransaction[];
  transactionsByHash: Record<string, DigitalCashTransaction>;
  txsOutByPKHash: Record<string, TxOut[]>;
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
      addTransaction: {
        prepare(laoId: Hash | string, rollCallId: Hash | string, transaction: DigitalCashTransaction): any {
          return { payload: { laoId: laoId.valueOf(), rollCallId: rollCallId.valueOf(), transaction: DigitalCashTransaction} };
        },
        reducer(
          state,
          action: PayloadAction<{
            laoId: string;
            rollCallId: string;
            transaction: DigitalCashTransaction;
          }>,
        ) {
          const { laoId, rollCallId, transaction } = action.payload;

          if(!(laoId in state.byLaoId)){
            state.byLaoId[laoId] = {
              byRCId: {},
            };
          }

          if(!(rollCallId in state.byLaoId[laoId])){
            state.byLaoId[laoId].byRCId[rollCallId] = {
              transactions: [],
              txsOutByPKHash: {},
              transactionsByHash: {},
            };
          }

          const rollCallState: DigitalCashReducerState = state.byLaoId[laoId].byRCId[rollCallId];
          // rollCallState.transactionsByHash[]
        }
      }
    }
  }
);
