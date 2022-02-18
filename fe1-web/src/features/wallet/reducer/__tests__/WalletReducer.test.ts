import 'jest-extended';
import { AnyAction } from 'redux';

import { walletReduce, clearWallet, setWallet } from '../WalletReducer';

const emptyState = {
  seed: undefined,
  mnemonic: undefined,
};

const filledState = {
  seed: '1234',
  mnemonic: 'alpha beta',
};

const wrongState = {
  seed: undefined,
  mnemonic: 'alpha beta',
};

describe('wallet reducer', () => {
  test('should return the initial state', () => {
    expect(walletReduce(undefined, {} as AnyAction)).toEqual(emptyState);
  });

  test('should handle the wallet being set', () => {
    expect(walletReduce({}, setWallet(filledState))).toEqual(filledState);
  });

  test('should return the empty set', () => {
    expect(walletReduce({}, setWallet(wrongState))).toEqual(emptyState);
  });

  test('should handle the wallet being set', () => {
    expect(walletReduce(filledState, clearWallet())).toEqual(emptyState);
  });
});
