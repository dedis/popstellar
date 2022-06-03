import 'jest-extended';
import '__tests__/utils/matchers';

import { mockKeyPair, mockPublicKey2 } from '__tests__/utils';
import { Hash, PopToken, PublicKey } from 'core/objects';
import STRINGS from 'resources/strings';

import { Transaction, TransactionJSON, TransactionState } from '../Transaction';
import { TransactionInput } from '../TransactionInput';
import { TransactionOutput } from '../TransactionOutput';

// region Mock value definitions
const mockPopToken = PopToken.fromState(mockKeyPair.toState());
const mockValue = 100;
const mockCBSig =
  '1E7yPEY7SwZ1NNxi_jN85-IvThR7GzUFs8-Lfwao44waoCf-qj5xLKnpzsc8yP0cFr5UNOIzsgKUYaSzv1gpDQ==';
const mockCBHash = 'FhlMNTEOqOzkKbe8RH00fmF-Op0S_ipowEn0nj402Ts=';
const validCoinbaseJSON: TransactionJSON = {
  version: 1,
  inputs: [
    {
      tx_out_hash: STRINGS.coinbase_hash,
      tx_out_index: 0,
      script: {
        type: 'Pay-to-Pubkey-Hash',
        pubkey: mockKeyPair.publicKey.valueOf(),
        sig: mockCBSig,
      },
    },
  ],
  outputs: [
    {
      value: mockValue,
      script: {
        type: 'Pay-to-Pubkey-Hash',
        pubkey_hash: Hash.fromPublicKey(mockKeyPair.publicKey).valueOf(),
      },
    },
  ],
  lock_time: 0,
};
const validCoinbaseState: TransactionState = {
  transactionId: mockCBHash.valueOf(),
  version: 1,
  inputs: [
    {
      txOutHash: STRINGS.coinbase_hash,
      txOutIndex: 0,
      script: {
        type: 'Pay-to-Pubkey-Hash',
        publicKey: mockKeyPair.publicKey.valueOf(),
        signature: mockCBSig,
      },
    },
  ],
  outputs: [
    {
      value: mockValue,
      script: {
        type: 'Pay-to-Pubkey-Hash',
        publicKeyHash: Hash.fromPublicKey(mockKeyPair.publicKey).valueOf(),
      },
    },
  ],
  lockTime: 0,
};
const validTransactionState0: TransactionState = {
  version: 1,
  inputs: [
    {
      txOutHash: 'FhlMNTEOqOzkKbe8RH00fmF-Op0S_ipowEn0nj402Ts=',
      txOutIndex: 0,
      script: {
        type: 'Pay-to-Pubkey-Hash',
        publicKey: 'J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=',
        signature:
          'rXV5aml3l5Yl9HmwEKEoRwjwGJw8rYcGwtEJkFk4FvAD9_3eZjTHGEIV4jPkKhmKRuv-hG5EgEXrLCgGJY6pBQ==',
      },
    },
  ],
  outputs: [
    {
      value: 100,
      script: {
        type: 'Pay-to-Pubkey-Hash',
        publicKeyHash: '-_qR4IHwsiq50raa8jURNArds54=',
      },
    },
  ],
  transactionId: '72AqOuKOSNuVsCEkWQ9gtfm8biBgUJyInOhMw4NqkGI=',
  lockTime: 0,
};
const validTransactionState1: TransactionState = {
  version: 1,
  inputs: [
    {
      txOutHash: 'FhlMNTEOqOzkKbe8RH00fmF-Op0S_ipowEn0nj402Ts=',
      txOutIndex: 0,
      script: {
        type: 'Pay-to-Pubkey-Hash',
        publicKey: 'J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=',
        signature:
          'bRhZp9bDfhCR5GVzNM6NYDPbOvOPDfnV_RN6Wo4UEosUPPcDImDkOfBxurzAxAd5DLABSkpm_oiW4PNJz2vaBg==',
      },
    },
  ],
  outputs: [
    {
      value: 50,
      script: {
        type: 'Pay-to-Pubkey-Hash',
        publicKeyHash: '-_qR4IHwsiq50raa8jURNArds54=',
      },
    },
    {
      value: 50,
      script: {
        type: 'Pay-to-Pubkey-Hash',
        publicKeyHash: '-_qR4IHwsiq50raa8jURNArds54=',
      },
    },
  ],
  transactionId: 'u62vOHqen-DEraUbJHnZhJ_qQPr4MPe9tvR6lZzmpJE=',
  lockTime: 0,
};

const randomTransactionState: TransactionState = {
  version: 1,
  inputs: [
    {
      txOutHash: 'EEEEETeOqOzklbe8RH00fmF-Op0S_ipowEn0nj00000=',
      txOutIndex: 0,
      script: {
        type: 'Pay-to-Pubkey-Hash',
        publicKey: 'EEEEETeOqOzklbe8RH00fmF-Op0S_ipowEn0nj00000=',
        signature:
          'WAY5aml3l5Yl9HmwEKEoRwjwGJw8rYcGwtEJkFk4FvAD9_3eZjTHGEIV4jPkKhmKRuv-hG5EgEXrLCgGJY6pBQ==',
      },
    },
  ],
  outputs: [
    {
      value: 100,
      script: {
        type: 'Pay-to-Pubkey-Hash',
        publicKeyHash: '-_NO4IHwsiq50raa8jURNArds54=',
      },
    },
  ],
  transactionId: '99AqOuKOSNuVsCEkWQ9gtfm8biBgUJyInOhMw4NqkGI=',
  lockTime: 0,
};
// endregion

describe('Transaction', () => {
  it('should be able to do a state round trip', () => {
    const coinbaseTransaction = Transaction.fromState(validCoinbaseState);
    expect(coinbaseTransaction.toState()).toEqual(validCoinbaseState);
  });
  it('should be able to do a JSON round trip', () => {
    const coinbaseTransaction = Transaction.fromJSON(validCoinbaseJSON, mockCBHash);
    expect(coinbaseTransaction.toJSON()).toEqual(validCoinbaseJSON);
  });
  it('should fail to create a transaction with invalid hash', () => {
    expect(() => Transaction.fromJSON(validCoinbaseJSON, 'hash')).toThrow(Error);
  });
  it('should be able to create a transaction from a valid partial transaction', () => {
    const transactionObject = {
      version: validCoinbaseState.version,
      lockTime: validCoinbaseState.lockTime,
      inputs: validCoinbaseState.inputs.map((inputState) => TransactionInput.fromState(inputState)),
      outputs: validCoinbaseState.outputs.map((outputState) =>
        TransactionOutput.fromState(outputState),
      ),
    };
    const coinbaseTransaction = new Transaction(transactionObject);
    expect(coinbaseTransaction.toState()).toEqual(validCoinbaseState);
  });
  it('should fail to create a transaction from a undefined object', () => {
    expect(() => new Transaction({})).toThrow(Error);
  });
  it('should fail to create a transaction from a partial transaction without version', () => {
    const transactionObject = {
      lockTime: validCoinbaseState.lockTime,
      inputs: validCoinbaseState.inputs.map((inputState) => TransactionInput.fromState(inputState)),
      outputs: validCoinbaseState.outputs.map((outputState) =>
        TransactionOutput.fromState(outputState),
      ),
    };
    expect(() => new Transaction(transactionObject)).toThrow(Error);
  });
  it('should fail to create a transaction from a partial transaction without lockTime', () => {
    const transactionObject = {
      version: validCoinbaseState.version,
      inputs: validCoinbaseState.inputs.map((inputState) => TransactionInput.fromState(inputState)),
      outputs: validCoinbaseState.outputs.map((outputState) =>
        TransactionOutput.fromState(outputState),
      ),
    };
    expect(() => new Transaction(transactionObject)).toThrow(Error);
  });
  it('should fail to create a transaction from a partial transaction without inputs', () => {
    const transactionObject = {
      lockTime: validCoinbaseState.lockTime,
      version: validCoinbaseState.version,
      outputs: validCoinbaseState.outputs.map((outputState) =>
        TransactionOutput.fromState(outputState),
      ),
    };
    expect(() => new Transaction(transactionObject)).toThrow(Error);
  });
  it('should fail to create a transaction from a partial transaction without outputs', () => {
    const transactionObject = {
      lockTime: validCoinbaseState.lockTime,
      version: validCoinbaseState.version,
      inputs: validCoinbaseState.inputs.map((inputState) => TransactionInput.fromState(inputState)),
    };
    expect(() => new Transaction(transactionObject)).toThrow(Error);
  });
  it('should fail to create a transaction from a partial transaction with empty inputs or outputs', () => {
    const transactionObject = {
      version: validCoinbaseState.version,
      lockTime: validCoinbaseState.lockTime,
      inputs: [],
      outputs: validCoinbaseState.outputs.map((outputState) =>
        TransactionOutput.fromState(outputState),
      ),
    };
    expect(() => new Transaction(transactionObject)).toThrow(Error);

    const transactionObject1 = {
      version: validCoinbaseState.version,
      lockTime: validCoinbaseState.lockTime,
      inputs: validCoinbaseState.inputs.map((inputState) => TransactionInput.fromState(inputState)),
      outputs: [],
    };
    expect(() => new Transaction(transactionObject1)).toThrow(Error);
  });
  it('should be able to hash a transaction correctly', () => {
    const coinbaseTransaction = Transaction.fromState({
      ...validCoinbaseState,
      transactionId: undefined,
    });
    expect(coinbaseTransaction.transactionId.valueOf()).toEqual(mockCBHash.valueOf());
  });
  it('should be able to create a coinbase transaction properly', () => {
    const coinbaseTransaction = Transaction.createCoinbase(
      mockKeyPair,
      mockKeyPair.publicKey,
      mockValue,
    );
    expect(coinbaseTransaction.transactionId.valueOf()).toEqual(mockCBHash.valueOf());
    expect(coinbaseTransaction.toJSON()).toEqual(validCoinbaseJSON);
    expect(coinbaseTransaction.toState()).toEqual(validCoinbaseState);
  });
  it('should validate a properly signed transaction', () => {
    const coinbaseTransaction = Transaction.createCoinbase(
      mockKeyPair,
      mockKeyPair.publicKey,
      mockValue,
    );
    const isValid = Transaction.isTransactionValid(coinbaseTransaction, mockKeyPair.publicKey);
    expect(isValid).toBeTrue();
  });
  it('should invalidate a badly signed transaction', () => {
    const coinbaseTransaction = Transaction.createCoinbase(
      mockKeyPair,
      mockKeyPair.publicKey,
      mockValue,
    );
    const isValid = Transaction.isTransactionValid(
      coinbaseTransaction,
      new PublicKey(mockPublicKey2),
    );
    expect(isValid).toBeFalse();
  });
  it('should be able to create a transaction from other valid transaction inputs', () => {
    const transaction = Transaction.create(
      mockPopToken,
      mockKeyPair.publicKey,
      mockValue,
      mockValue,
      [validCoinbaseState],
    );
    expect(transaction.toState()).toEqual(validTransactionState0);
  });
  it('should be able to create a transaction with split outputs', () => {
    const transaction = Transaction.create(
      mockPopToken,
      mockKeyPair.publicKey,
      mockValue,
      mockValue / 2,
      [validCoinbaseState],
    );
    expect(transaction.toState()).toEqual(validTransactionState1);
  });
  it('should fail to create a transaction from empty transaction inputs', () => {
    expect(() =>
      Transaction.create(mockPopToken, mockKeyPair.publicKey, mockValue, mockValue, []),
    ).toThrow(Error);
  });
  it('should fail to create a transaction from invalid transaction inputs', () => {
    expect(() =>
      Transaction.create(mockPopToken, mockKeyPair.publicKey, mockValue, mockValue, [
        randomTransactionState,
      ]),
    ).toThrow(Error);
  });
});
