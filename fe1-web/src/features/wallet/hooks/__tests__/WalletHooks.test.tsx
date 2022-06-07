import { describe } from '@jest/globals';
import { renderHook } from '@testing-library/react-hooks';
import React from 'react';

import { mockKeyPair, mockLaoId, mockLaoIdHash } from "__tests__/utils";
import FeatureContext from 'core/contexts/FeatureContext';
import { mockRollCall, mockRollCallState } from 'features/rollCall/__tests__/utils';
import { WalletReactContext, WALLET_FEATURE_IDENTIFIER } from 'features/wallet/interface';

import { WalletHooks } from '../index';

const getEventById = jest.fn();
const rollCallByIdMapByLaoId = {
  [mockLaoId]: {
    [mockRollCallState.id]: mockRollCall,
  },
};
const useRollCallsByLaoId = jest.fn(() => rollCallByIdMapByLaoId);
const getLaoOrganizer = jest.fn(() => mockKeyPair.publicKey);

const contextValue = {
  [WALLET_FEATURE_IDENTIFIER]: {
    useCurrentLaoId: () => mockLaoIdHash,
    getEventById,
    useRollCallsByLaoId,
    getLaoOrganizer,
  } as WalletReactContext,
};

const wrapper = ({ children }: { children: React.ReactChildren }) => (
  <FeatureContext.Provider value={contextValue}>{children}</FeatureContext.Provider>
);

beforeEach(() => {
  jest.clearAllMocks();
});

describe('WalletHooks', () => {
  describe('useCurrentLaoId', () => {
    it('should return the current lao id', () => {
      const { result } = renderHook(() => WalletHooks.useCurrentLaoId(), { wrapper });
      expect(result.current).toEqual(mockLaoIdHash);
    });
  });

  describe('WalletHooks.useRollCallsByLaoIdSelector', () => {
    it('should return a map from lao ids to a map from roll call ids to roll call instances', () => {
      const { result } = renderHook(() => WalletHooks.useRollCallsByLaoId(), { wrapper });
      expect(result.current).toEqual(rollCallByIdMapByLaoId);
    });
  });
});
