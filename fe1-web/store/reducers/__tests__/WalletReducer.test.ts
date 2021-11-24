import 'jest-extended';
import { AnyAction } from 'redux';
import { walletReducer, clearWallet, setWallet } from '../WalletReducer';

const emptyState = {
  seed: undefined,
  mnemonic: undefined,
};

const filledState = {
  seed: '1234',
  mnemonic: 'alpha beta',
};

test('should return the initial state', () => {
  expect(walletReducer(undefined, {} as AnyAction))
    .toEqual(emptyState);
});

test('should handle the wallet being set', () => {
  expect(walletReducer({}, setWallet(filledState)))
    .toEqual(filledState);
});

test('should handle the wallet being set', () => {
  expect(walletReducer(filledState, clearWallet()))
    .toEqual(emptyState);
});
