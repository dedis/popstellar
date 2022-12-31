import { describe } from '@jest/globals';
import { renderHook } from '@testing-library/react-hooks';
import React from 'react';

import { mockKeyPair, mockLao, serializedMockLaoId, mockLaoId, mockLaoName } from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';
import { Hash } from 'core/objects';
import { mockRollCallToken } from 'features/digital-cash/__tests__/utils';
import { mockRollCall, mockRollCallState } from 'features/rollCall/__tests__/utils';
import { WalletReactContext, WALLET_FEATURE_IDENTIFIER } from 'features/wallet/interface';

import { WalletHooks } from '../index';

const getEventById = jest.fn();
const rollCallByIdMapByLaoId = {
  [serializedMockLaoId]: {
    [mockRollCallState.id]: mockRollCall,
  },
};

const getLaoOrganizer = jest.fn(() => mockKeyPair.publicKey);
const useRollCallsByLaoId = jest.fn((laoId) => rollCallByIdMapByLaoId[laoId]);
const useRollCallTokensByLaoId = jest.fn(() => [mockRollCallToken]);

const allLaoIds: Hash[] = [mockLaoId];
const laoNameById = { [serializedMockLaoId]: mockLaoName };

const contextValue = {
  [WALLET_FEATURE_IDENTIFIER]: {
    useCurrentLaoId: () => mockLaoId,
    useCurrentLao: () => mockLao,
    useConnectedToLao: () => false,
    getEventById,
    useRollCallsByLaoId,
    getLaoOrganizer,
    useLaoIds: () => allLaoIds,
    useNamesByLaoId: () => laoNameById,
    useRollCallTokensByLaoId,
  } as WalletReactContext,
};

const wrapper = ({ children }: { children: React.ReactNode }) => (
  <FeatureContext.Provider value={contextValue}>{children}</FeatureContext.Provider>
);

beforeEach(() => {
  jest.clearAllMocks();
});

describe('WalletHooks', () => {
  describe('useCurrentLaoId', () => {
    it('should return the current lao id', () => {
      const { result } = renderHook(() => WalletHooks.useCurrentLaoId(), { wrapper });
      expect(result.current).toEqual(mockLaoId);
    });
  });

  describe('useCurrentLao', () => {
    it('should return the current lao', () => {
      const { result } = renderHook(() => WalletHooks.useCurrentLao(), { wrapper });
      expect(result.current).toEqual(mockLao);
    });
  });

  describe('useConnectedToLao', () => {
    it('should return whether currently connected to a lao', () => {
      const { result } = renderHook(() => WalletHooks.useConnectedToLao(), { wrapper });
      expect(result.current).toBeFalse();
    });
  });

  describe('useRollCallsByLaoId', () => {
    it('should return a map from lao ids to a map from roll call ids to roll call instances', () => {
      const { result } = renderHook(() => WalletHooks.useRollCallsByLaoId(mockLaoId), {
        wrapper,
      });
      expect(result.current).toEqual(rollCallByIdMapByLaoId[serializedMockLaoId]);
    });
  });

  describe('useRollCallTokensByLaoId', () => {
    it('should return the correct function', () => {
      const { result } = renderHook(() => WalletHooks.useRollCallTokensByLaoId(mockLaoId), {
        wrapper,
      });
      expect(result.current).toEqual([mockRollCallToken]);
    });
  });
});
