import 'jest-extended';
import { AnyAction } from 'redux';
import { reducer, clearWallet, setWallet } from '../WalletReducer';

const emptyState = {
  seed: undefined,
  mnemonic: undefined,
};

const filledState = {
  seed: '1234',
  mnemonic: 'alpha beta',
};

test('should return the initial state', () => {
  expect(reducer(undefined, {} as AnyAction))
    .toEqual(emptyState);
});

test('should handle the wallet being set', () => {
  expect(reducer({}, setWallet(filledState)))
    .toEqual(filledState);
});

test('should handle the wallet being set', () => {
  expect(reducer(filledState, clearWallet()))
    .toEqual(emptyState);
});
