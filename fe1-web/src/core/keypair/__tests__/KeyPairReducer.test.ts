import 'jest-extended';

import { AnyAction } from 'redux';

import { mockKeyPair } from '__tests__/utils';

import { keyPairReduce, setKeyPair } from '../KeyPairReducer';

const emptyState = {
  keyPair: undefined,
};

const filledState = {
  keyPair: mockKeyPair.toState(),
};

describe('KeyPairReducer', () => {
  it('should return the initial state', () => {
    expect(keyPairReduce(undefined, {} as AnyAction)).toEqual(emptyState);
  });

  it('should handle the keyPair being set', () => {
    const newState = keyPairReduce(emptyState, setKeyPair(mockKeyPair.toState()));
    expect(newState).toEqual(filledState);
  });
});
