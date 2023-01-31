import { beforeAll } from '@jest/globals';

import { configureTestFeatures, mockLao, mockLaoId } from '__tests__/utils';
import { dispatch } from 'core/redux';
import { OpenedLaoStore } from 'features/lao/store';

import { getCurrentLao, getCurrentLaoId } from '../lao';

// many functions access the global store, hence it has to be set up first
beforeAll(() => {
  configureTestFeatures();
});

beforeEach(() => {
  // clear data in the redux store
  dispatch({ type: 'CLEAR_STORAGE', value: {} });
});

describe('getCurrentLao', () => {
  it('should return the current lao if there is one', () => {
    OpenedLaoStore.store(mockLao);
    expect(getCurrentLao()).toEqual(mockLao);
  });

  it('should throw if there is no current lao', () => {
    expect(getCurrentLao).toThrowError();
  });
});

describe('getCurrentLaoId', () => {
  it('should return the current lao id if there is one', () => {
    OpenedLaoStore.store(mockLao);
    expect(getCurrentLaoId()).toEqual(mockLaoId);
  });

  it('should return undefined if there is no current lao', () => {
    expect(getCurrentLaoId()).toBeUndefined();
  });
});
