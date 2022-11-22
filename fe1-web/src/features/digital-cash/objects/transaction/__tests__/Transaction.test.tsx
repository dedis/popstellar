import 'jest-extended';
import '__tests__/utils/matchers';

import { mockKeyPair, mockPublicKey2 } from '__tests__/utils';
import { Hash, PopToken, PublicKey, Timestamp } from 'core/objects';
import { COINBASE_HASH, SCRIPT_TYPE } from 'resources/const';

import {
  mockCBHash,
  mockCBSig,
  mockCoinbaseTransactionJSON,
  mockTransactionState,
  mockTransactionValue,
} from '../../../__tests__/utils';
import { Transaction, TransactionState } from '../Transaction';
import { TransactionInput } from '../TransactionInput';
import { TransactionOutput } from '../TransactionOutput';

// region Mock value definitions
const mockPopToken = PopToken.fromState(mockKeyPair.toState());

const validCoinbaseState: TransactionState = {
  version: 1,
  inputs: [
    {
      txOutHash: COINBASE_HASH,
      txOutIndex: 0,
      script: {
        type: SCRIPT_TYPE,
        publicKey: mockKeyPair.publicKey.valueOf(),
        signature: mockCBSig,
      },
    },
  ],
  outputs: [
    {
      value: mockTransactionValue,
      script: {
        type: SCRIPT_TYPE,
        publicKeyHash: Hash.fromPublicKey(mockKeyPair.publicKey).valueOf(),
      },
    },
  ],
  transactionId: mockCBHash.valueOf(),
  lockTime: 0,
};

const validTransactionState1: TransactionState = {
  version: 1,
  inputs: [
    {
      txOutHash: mockCBHash.valueOf(),
      txOutIndex: 0,
      script: {
        type: SCRIPT_TYPE,
        publicKey: mockKeyPair.publicKey.valueOf(),
        signature:
          'mPbWaEkjY-Lkf__nYDURTzhQhc0pIcnrwMMBXtDObYSJeO0rC769_mDx3eMbRWpO94_aS3OJqUOlZMttbSQIAQ==',
      },
    },
  ],
  outputs: [
    {
      value: 50,
      script: {
        type: SCRIPT_TYPE,
        publicKeyHash: '-_qR4IHwsiq50raa8jURNArds54=',
      },
    },
    {
      value: 50,
      script: {
        type: SCRIPT_TYPE,
        publicKeyHash: '-_qR4IHwsiq50raa8jURNArds54=',
      },
    },
  ],
  transactionId: 'Fx9iFMM4orpZkdUjHdcvtkW3QGpm-AyWkygsOcuCVZQ=',
  lockTime: 0,
};

const randomTransactionState: TransactionState = {
  version: 1,
  inputs: [
    {
      txOutHash: 'EEEEETeOqOzklbe8RH00fmF-Op0S_ipowEn0nj00000=',
      txOutIndex: 0,
      script: {
        type: SCRIPT_TYPE,
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
        type: SCRIPT_TYPE,
        publicKeyHash: '-_NO4IHwsiq50raa8jURNArds54=',
      },
    },
  ],
  transactionId: '99AqOuKOSNuVsCEkWQ9gtfm8biBgUJyInOhMw4NqkGI=',
  lockTime: 0,
};

const mockTransactionRecordByHash: Record<string, TransactionState> = {
  [mockCBHash.valueOf()]: Transaction.fromJSON(mockCoinbaseTransactionJSON, mockCBHash).toState(),
};
// endregion

describe('Transaction', () => {
  it('should be able to do a state round trip', () => {
    const coinbaseTransaction = Transaction.fromState(validCoinbaseState);
    expect(coinbaseTransaction.toState()).toEqual(validCoinbaseState);
  });

  it('should be able to do a JSON round trip', () => {
    const coinbaseTransaction = Transaction.fromJSON(mockCoinbaseTransactionJSON, mockCBHash);
    expect(coinbaseTransaction.toJSON()).toEqual(mockCoinbaseTransactionJSON);
  });

  it('should fail to create a transaction with invalid hash', () => {
    expect(() => Transaction.fromJSON(mockCoinbaseTransactionJSON, new Hash('hash'))).toThrow(
      Error,
    );
  });

  it('should be able to create a transaction from a valid partial transaction', () => {
    const transactionObject = {
      version: validCoinbaseState.version,
      lockTime: Timestamp.fromState(validCoinbaseState.lockTime),
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
      lockTime: Timestamp.fromState(validCoinbaseState.lockTime),
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
      lockTime: Timestamp.fromState(validCoinbaseState.lockTime),
      version: validCoinbaseState.version,
      outputs: validCoinbaseState.outputs.map((outputState) =>
        TransactionOutput.fromState(outputState),
      ),
    };
    expect(() => new Transaction(transactionObject)).toThrow(Error);
  });

  it('should fail to create a transaction from a partial transaction without outputs', () => {
    const transactionObject = {
      lockTime: Timestamp.fromState(validCoinbaseState.lockTime),
      version: validCoinbaseState.version,
      inputs: validCoinbaseState.inputs.map((inputState) => TransactionInput.fromState(inputState)),
    };
    expect(() => new Transaction(transactionObject)).toThrow(Error);
  });

  it('should fail to create a transaction from a partial transaction with empty inputs or outputs', () => {
    const transactionObject = {
      version: validCoinbaseState.version,
      lockTime: Timestamp.fromState(validCoinbaseState.lockTime),
      inputs: [],
      outputs: validCoinbaseState.outputs.map((outputState) =>
        TransactionOutput.fromState(outputState),
      ),
    };
    expect(() => new Transaction(transactionObject)).toThrow(Error);

    const transactionObject1 = {
      version: validCoinbaseState.version,
      lockTime: Timestamp.fromState(validCoinbaseState.lockTime),
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
      [mockKeyPair.publicKey],
      mockTransactionValue,
    );
    expect(coinbaseTransaction.transactionId.valueOf()).toEqual(mockCBHash.valueOf());
    expect(coinbaseTransaction.toJSON()).toEqual(mockCoinbaseTransactionJSON);
    expect(coinbaseTransaction.toState()).toEqual(validCoinbaseState);
  });

  it('should be able to create a transaction from other valid transaction inputs', () => {
    const transaction = Transaction.create(
      mockPopToken,
      mockKeyPair.publicKey,
      mockTransactionValue,
      mockTransactionValue,
      [validCoinbaseState],
    );
    expect(transaction.toState()).toEqual(mockTransactionState);
  });

  it('should be able to create a transaction with split outputs', () => {
    const transaction = Transaction.create(
      mockPopToken,
      mockKeyPair.publicKey,
      mockTransactionValue,
      mockTransactionValue / 2,
      [validCoinbaseState],
    );
    expect(transaction.toState()).toEqual(validTransactionState1);
  });

  it('should fail to create a transaction from empty transaction inputs', () => {
    expect(() =>
      Transaction.create(
        mockPopToken,
        mockKeyPair.publicKey,
        mockTransactionValue,
        mockTransactionValue,
        [],
      ),
    ).toThrow(Error);
  });

  it('should fail to create a transaction from invalid transaction inputs', () => {
    expect(() =>
      Transaction.create(
        mockPopToken,
        mockKeyPair.publicKey,
        mockTransactionValue,
        mockTransactionValue,
        [randomTransactionState],
      ),
    ).toThrow(Error);
  });

  describe('validation', () => {
    it('should validate a properly signed coinbase transaction', () => {
      const coinbaseTransaction = Transaction.createCoinbase(
        mockKeyPair,
        [mockKeyPair.publicKey],
        mockTransactionValue,
      );
      const isValid = coinbaseTransaction.checkTransactionValidity(
        mockKeyPair.publicKey,
        mockTransactionRecordByHash,
      );
      expect(isValid).toBeTrue();
    });

    it('should invalidate a badly signed transaction', () => {
      const coinbaseTransaction = Transaction.createCoinbase(
        mockKeyPair,
        [mockKeyPair.publicKey],
        mockTransactionValue,
      );
      const isValid = coinbaseTransaction.checkTransactionValidity(
        new PublicKey(mockPublicKey2),
        mockTransactionRecordByHash,
      );
      expect(isValid).toBeFalse();
    });

    it('should validate a properly signed transaction', () => {
      const transaction = Transaction.create(
        mockPopToken,
        mockKeyPair.publicKey,
        mockTransactionValue,
        mockTransactionValue,
        [validCoinbaseState],
      );
      const isValid = transaction.checkTransactionValidity(
        new PublicKey(mockPublicKey2),
        mockTransactionRecordByHash,
      );
      expect(isValid).toBeTrue();
    });
  });
});
