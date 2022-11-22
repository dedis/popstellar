import 'jest-extended';

import { AnyAction } from 'redux';

import { mockLaoId, serializedMockLaoId } from '__tests__/utils';
import { PublicKey } from 'core/objects';

import {
  mockCBHash,
  mockCoinbaseTransactionJSON,
  mockKPHash,
  mockTransactionValue,
} from '../../__tests__/utils';
import { Transaction } from '../../objects/transaction';
import {
  digitalCashReduce,
  addTransaction,
  makeBalanceSelector,
  DIGITAL_CASH_REDUCER_PATH,
} from '../DigitalCashReducer';

const mockTransaction = Transaction.fromJSON(mockCoinbaseTransactionJSON, mockCBHash);

const emptyState = {
  byLaoId: {},
};

const filledState = {
  byLaoId: {
    [serializedMockLaoId.valueOf()]: {
      balances: {
        [mockKPHash.valueOf()]: mockTransactionValue,
      },
      allTransactionsHash: [mockTransaction.transactionId],
      transactionsByHash: {
        [mockTransaction.transactionId.serialize()]: mockTransaction,
      },
      transactionsByPubHash: {
        [mockKPHash.valueOf()]: [mockTransaction.transactionId],
      },
    },
  },
};

describe('Digital Cash reducer', () => {
  it('should return the initial state', () => {
    expect(digitalCashReduce(undefined, {} as AnyAction)).toEqual(emptyState);
  });

  it('should handle a transaction being added from empty state', () => {
    expect(digitalCashReduce(emptyState, addTransaction(mockLaoId, mockTransaction))).toEqual(
      filledState,
    );
  });

  describe('make balance selector', () => {
    it('should be able to recover a balance', () => {
      expect(
        makeBalanceSelector(
          mockLaoId,
          new PublicKey(mockCoinbaseTransactionJSON.inputs[0].script.pubkey),
        )({ [DIGITAL_CASH_REDUCER_PATH]: filledState }),
      ).toEqual(100);
    });

    it('should return 0 when public key is not found', () => {
      expect(
        makeBalanceSelector(
          mockLaoId,
          new PublicKey('pubkey'),
        )({ [DIGITAL_CASH_REDUCER_PATH]: filledState }),
      ).toEqual(0);
    });
  });
});
