import {
  configureTestFeatures,
  mockCBHash,
  mockCoinbaseTransactionJSON,
  mockKeyPair,
  mockLaoId,
  mockPopToken,
  mockTransactionState,
  mockTransactionValue,
} from '__tests__/utils';
import { ActionType, MessageData, ObjectType } from 'core/network/jsonrpc/messages';
import { publish } from 'core/network/JsonRpcApi';
import { Hash, PopToken } from 'core/objects';
import { PostTransaction } from 'features/wallet/network/messages';
import { Transaction } from 'features/wallet/objects/transaction';
import { DigitalCashStore } from 'features/wallet/store';

import { requestSendTransaction, requestCoinbaseTransaction } from '../DigitalCashMessageApi';

jest.mock('features/wallet/store/DigitalCashStore');
const getTransactionsByPublicKeyMock = DigitalCashStore.getTransactionsByPublicKey as jest.Mock;

jest.mock('core/network/JsonRpcApi');
const publishMock = publish as jest.Mock;

const mockPopTokenKeyPair = PopToken.fromState(mockKeyPair.toState());

const mockPostCoinbase = new PostTransaction({
  transaction_id: new Hash(mockCBHash),
  transaction: mockCoinbaseTransactionJSON,
});
const mockCoinbaseState = Transaction.fromJSON(mockCoinbaseTransactionJSON, mockCBHash).toState();

const mockTransaction = Transaction.fromState(mockTransactionState);
const mockPost = new PostTransaction({
  transaction_id: mockTransaction.transactionId,
  transaction: mockTransaction.toJSON(),
});

const checkMessageCorrectness = (message: MessageData) => {
  expect(message.object).toBe(ObjectType.COIN);
  expect(message.action).toBe(ActionType.POST_TRANSACTION);
};

beforeAll(() => {
  configureTestFeatures();
});

beforeEach(() => {
  publishMock.mockClear();
  getTransactionsByPublicKeyMock.mockClear();
  getTransactionsByPublicKeyMock.mockReturnValue([mockCoinbaseState]);
});

describe('Digital Cash Message Api', () => {
  it('should create a correct coinbase transaction request', async () => {
    await requestCoinbaseTransaction(
      mockKeyPair,
      [mockKeyPair.publicKey],
      mockTransactionValue,
      new Hash(mockLaoId),
    );

    expect(publishMock).toBeCalledTimes(1);
    const [channel, msgData] = publishMock.mock.calls[0];
    expect(channel).toBe(`/root/${mockLaoId}/coin`);

    const postTransaction = msgData as PostTransaction;
    expect(postTransaction).toEqual(mockPostCoinbase);
    checkMessageCorrectness(postTransaction);
  });

  it('should create a correct transaction request', async () => {
    await requestSendTransaction(
      mockPopTokenKeyPair,
      mockKeyPair.publicKey,
      mockTransactionValue,
      new Hash(mockLaoId),
    );

    expect(publishMock).toBeCalledTimes(1);
    const [channel, msgData] = publishMock.mock.calls[0];
    expect(channel).toBe(`/root/${mockLaoId}/coin`);

    const postTransaction = msgData as PostTransaction;
    expect(postTransaction).toEqual(mockPost);
    checkMessageCorrectness(postTransaction);
  });

  it('should throw an error when no transaction out found', async () => {
    getTransactionsByPublicKeyMock.mockReturnValue([]);
    await expect(
      requestSendTransaction(
        mockPopToken,
        mockKeyPair.publicKey,
        mockTransactionValue,
        new Hash(mockLaoId),
      ),
    ).rejects.toBeInstanceOf(Error);
  });

  it('should throw an error with amount greater than balance', async () => {
    await expect(
      requestSendTransaction(
        mockPopToken,
        mockKeyPair.publicKey,
        mockTransactionValue + 10,
        new Hash(mockLaoId),
      ),
    ).rejects.toBeInstanceOf(Error);
  });

  it('should throw an error with negative amount', async () => {
    await expect(
      requestSendTransaction(
        mockPopToken,
        mockKeyPair.publicKey,
        -mockTransactionValue,
        new Hash(mockLaoId),
      ),
    ).rejects.toBeInstanceOf(Error);
  });
});
