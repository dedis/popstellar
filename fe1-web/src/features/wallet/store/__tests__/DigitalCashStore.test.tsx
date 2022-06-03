import 'jest-extended';

import {
  mockCBHash,
  mockCoinbaseTransactionJSON,
  mockKeyPair,
  mockKPHash,
  mockLaoId,
  mockTransactionValue,
} from '../../../../__tests__/utils';
import { Transaction } from '../../objects/transaction';
import { getDigitalCashState } from '../../reducer';
import { DigitalCashStore } from '../DigitalCashStore';

const mockTransaction = Transaction.fromJSON(mockCoinbaseTransactionJSON, mockCBHash).toState();

const filledState = {
  byLaoId: {
    [mockLaoId.valueOf()]: {
      balances: {
        [mockKPHash.valueOf()]: mockTransactionValue,
      },
      transactions: [mockTransaction],
      transactionsByHash: {
        [mockTransaction.transactionId!]: mockTransaction,
      },
      transactionsByPubHash: {
        [mockKPHash.valueOf()]: [mockTransaction],
      },
    },
  },
};

jest.mock('features/wallet/reducer/DigitalCashReducer');

beforeAll(() => {
  (getDigitalCashState as jest.Mock).mockReturnValue(filledState);
});

describe('Digital Cash Store', () => {
  it('should be able to recover a balance', () => {
    expect(DigitalCashStore.getBalance(mockLaoId, mockKeyPair.publicKey.valueOf())).toEqual(
      mockTransactionValue,
    );
  });
  it('should recover 0 as a balance if public key is not found', () => {
    expect(DigitalCashStore.getBalance(mockLaoId, 'pk')).toEqual(0);
  });
  it('should recover 0 as a balance if lao id is not found', () => {
    expect(DigitalCashStore.getBalance('mockId', mockKeyPair.publicKey.valueOf())).toEqual(0);
  });
  it('should recover the transaction by public key correctly', () => {
    expect(
      DigitalCashStore.getTransactionsByPublicKey(
        mockLaoId.valueOf(),
        mockKeyPair.publicKey.valueOf(),
      ),
    ).toEqual([mockTransaction]);
  });
  it('should recover an empty array if lao id is not found', () => {
    expect(
      DigitalCashStore.getTransactionsByPublicKey('mockId', mockKeyPair.publicKey.valueOf()),
    ).toEqual([]);
  });
  it('should recover an empty array if public key is not found', () => {
    expect(DigitalCashStore.getTransactionsByPublicKey('mockId', 'publicKey')).toEqual([]);
  });
});
