import { describe } from '@jest/globals';
import { renderHook } from '@testing-library/react-hooks';
import React from 'react';

import { mockKeyPair, mockLao, mockLaoId, mockLaoIdHash, mockLaoName } from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';
import { Hash } from 'core/objects';
import { mockRollCallToken } from 'features/digital-cash/__tests__/utils';
import { mockRollCall, mockRollCallState } from 'features/rollCall/__tests__/utils';
import {
  WalletFeature,
  WalletReactContext,
  WALLET_FEATURE_IDENTIFIER,
} from 'features/wallet/interface';

import { WalletHooks } from '../index';

const getEventById = jest.fn();
const rollCallByIdMapByLaoId = {
  [mockLaoId]: {
    [mockRollCallState.id]: mockRollCall,
  },
};

const getLaoOrganizer = jest.fn(() => mockKeyPair.publicKey);
const useRollCallsByLaoId = jest.fn((laoId) => rollCallByIdMapByLaoId[laoId]);
const useRollCallTokensByLaoId = jest.fn(() => [mockRollCallToken]);

const allLaoIds: Hash[] = [mockLaoIdHash];
const laoNameById = { [mockLaoId]: mockLaoName };

const walletItemGenerators: WalletFeature.WalletItemGenerator[] = [];
const walletNavigationScreens: WalletFeature.WalletScreen[] = [];

const contextValue = {
  [WALLET_FEATURE_IDENTIFIER]: {
    useAssertCurrentLaoId: () => mockLaoIdHash,
    useCurrentLao: () => mockLao,
    useConnectedToLao: () => false,
    getEventById,
    useRollCallsByLaoId,
    getLaoOrganizer,
    useLaoIds: () => allLaoIds,
    useNamesByLaoId: () => laoNameById,
    walletItemGenerators,
    walletNavigationScreens,
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
  describe('walletItemGenerators', () => {
    it('should return the current wallet item generators', () => {
      const { result } = renderHook(() => WalletHooks.useWalletItemGenerators(), { wrapper });
      expect(result.current).toBe(walletItemGenerators);
    });
  });

  describe('walletNavigationScreens', () => {
    it('should return the current wallet navigation screens', () => {
      const { result } = renderHook(() => WalletHooks.useWalletNavigationScreens(), { wrapper });
      expect(result.current).toBe(walletNavigationScreens);
    });
  });

  describe('useAssertCurrentLaoId', () => {
    it('should return the current lao id', () => {
      const { result } = renderHook(() => WalletHooks.useAssertCurrentLaoId(), { wrapper });
      expect(result.current).toEqual(mockLaoIdHash);
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
      const { result } = renderHook(() => WalletHooks.useRollCallsByLaoId(mockLaoId), { wrapper });
      expect(result.current).toEqual(rollCallByIdMapByLaoId[mockLaoId]);
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
