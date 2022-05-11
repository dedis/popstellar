import { describe } from '@jest/globals';
import { renderHook } from '@testing-library/react-hooks';
import React from 'react';

import { mockLaoIdHash } from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';
import { WalletReactContext, WALLET_FEATURE_IDENTIFIER } from 'features/wallet/interface';

import { WalletHooks } from '../index';

const getEventById = jest.fn();
const eventByTypeSelector = jest.fn();
const makeEventByTypeSelector = jest.fn(() => eventByTypeSelector);

const contextValue = {
  [WALLET_FEATURE_IDENTIFIER]: {
    useCurrentLaoId: () => mockLaoIdHash,
    getEventById,
    makeEventByTypeSelector,
  } as WalletReactContext,
};

const wrapper = ({ children }: { children: React.ReactChildren }) => (
  <FeatureContext.Provider value={contextValue}>{children}</FeatureContext.Provider>
);

beforeEach(() => {
  jest.clearAllMocks();
});

describe('Wallet hooks', () => {
  describe('WalletHooks.useCurrentLaoId', () => {
    it('should return the current lao id', () => {
      const { result } = renderHook(() => WalletHooks.useCurrentLaoId(), { wrapper });
      expect(result.current).toEqual(mockLaoIdHash);
    });
  });

  describe('WalletHooks.useEventByTypeSelector', () => {
    it('should return the current lao id', () => {
      const { result } = renderHook(() => WalletHooks.useEventByTypeSelector('type'), { wrapper });
      expect(result.current).toEqual(eventByTypeSelector);
      expect(makeEventByTypeSelector).toHaveBeenCalledWith('type');
      expect(makeEventByTypeSelector).toHaveBeenCalledTimes(1);
    });
  });
});
