import {
  mockCBHash,
  mockCoinbaseTransactionJSON,
  mockKeyPair,
  mockLaoId,
  mockPublicKey2,
  mockTransactionState,
} from '__tests__/utils';
import { ActionType, ObjectType } from 'core/network/jsonrpc/messages';
import { Base64UrlData, Hash, Signature, Timestamp } from 'core/objects';
import { PostTransaction } from 'features/wallet/network/messages';
import { Transaction } from 'features/wallet/objects/transaction';
import { DigitalCashStore } from 'features/wallet/store';

import { handleTransactionPost } from '../DigitalCashHandler';

jest.mock('features/wallet/store/DigitalCashStore');

const mockCoinbaseTransaction = Transaction.fromJSON(mockCoinbaseTransactionJSON, mockCBHash);
const mockCoinbasePost = new PostTransaction({
  transaction_id: mockCoinbaseTransaction.transactionId,
  transaction: mockCoinbaseTransaction.toJSON(),
});

const mockTransaction = Transaction.fromState(mockTransactionState);
const mockPost = new PostTransaction({
  transaction_id: mockTransaction.transactionId,
  transaction: mockTransaction.toJSON(),
});

const badActionPost: PostTransaction = {
  ...mockPost,
  action: ActionType.CLOSE,
};
const badObjectPost: PostTransaction = {
  ...mockPost,
  object: ObjectType.CHIRP,
};

const createMockMessage = (post: PostTransaction) => {
  return {
    channel: '',
    data: Base64UrlData.fromBase64(''),
    message_id: new Hash('mockId'),
    receivedAt: new Timestamp(0),
    receivedFrom: '',
    sender: mockKeyPair.publicKey,
    signature: new Signature('signature'),
    witness_signatures: [],
    laoId: new Hash(mockLaoId),
    messageData: post,
  };
};

const mockAddTransaction = jest.fn();
const mockGetLaoOrganizer = jest.fn().mockReturnValue(mockKeyPair.publicKey);

(DigitalCashStore.getTransactionsById as jest.Mock).mockReturnValue({
  [mockCBHash.valueOf()]: mockCoinbaseTransaction,
});

describe('DigitalCash handler', () => {
  it('should correctly handle message transaction', () => {
    const mockMessage = createMockMessage(mockPost);
    expect(handleTransactionPost(mockAddTransaction, mockGetLaoOrganizer)(mockMessage)).toBeTrue();

    expect(mockAddTransaction).toHaveBeenCalledTimes(1);
    const [laoId, transaction] = mockAddTransaction.mock.calls[0];
    expect(laoId.valueOf()).toBe(mockLaoId);
    expect(transaction.toState()).toEqual(mockTransactionState);
  });

  it('should return false when action does not correspond', () => {
    const mockMessage = createMockMessage(badActionPost);
    expect(handleTransactionPost(mockAddTransaction, mockGetLaoOrganizer)(mockMessage)).toBeFalse();
  });

  it('should return false when object does not correspond', () => {
    const mockMessage = createMockMessage(badObjectPost);
    expect(handleTransactionPost(mockAddTransaction, mockGetLaoOrganizer)(mockMessage)).toBeFalse();
  });

  it('should return false when laoId is not defined', () => {
    const mockMessage = {
      ...createMockMessage(mockPost),
      laoId: undefined,
    };
    expect(handleTransactionPost(mockAddTransaction, mockGetLaoOrganizer)(mockMessage)).toBeFalse();
  });
  it('should return false when organizer did not sign the coinbase transaction', () => {
    const mockMessage = createMockMessage(mockCoinbasePost);
    expect(
      handleTransactionPost(
        mockAddTransaction,
        mockGetLaoOrganizer.mockReturnValue(mockPublicKey2),
      )(mockMessage),
    ).toBeFalse();
  });
});
