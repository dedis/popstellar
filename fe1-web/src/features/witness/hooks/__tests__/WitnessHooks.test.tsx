import { describe } from '@jest/globals';
import { renderHook } from '@testing-library/react-hooks';
import React from 'react';

import { mockLaoId } from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';
import { WitnessReactContext, WITNESS_FEATURE_IDENTIFIER } from 'features/witness/interface';

import { WitnessHooks } from '../WitnessHooks';

const addNotification = jest.fn();
const discardNotifications = jest.fn();
const markNotificationAsRead = jest.fn();

const contextValue = {
  [WITNESS_FEATURE_IDENTIFIER]: {
    enabled: true,
    useCurrentLaoId: () => mockLaoId,
    useConnectedToLao: () => true,
    addNotification,
    discardNotifications,
    markNotificationAsRead,
  } as WitnessReactContext,
};

// setup mock store

const wrapper = ({ children }: { children: React.ReactNode }) => (
  <FeatureContext.Provider value={contextValue}>{children}</FeatureContext.Provider>
);

describe('WitnessHooks', () => {
  describe('useCurrentLaoId', () => {
    it('should return the correct value', () => {
      const { result } = renderHook(() => WitnessHooks.useCurrentLaoId(), {
        wrapper,
      });
      expect(result.current).toEqual(mockLaoId);
    });
  });

  describe('useConnectedToLao', () => {
    it('should return the correct value', () => {
      const { result } = renderHook(() => WitnessHooks.useConnectedToLao(), {
        wrapper,
      });
      expect(result.current).toBeTrue();
    });
  });

  describe('useDiscardNotifications', () => {
    it('should return the correct function', () => {
      const { result } = renderHook(() => WitnessHooks.useDiscardNotifications(), {
        wrapper,
      });
      expect(result.current).toEqual(discardNotifications);
    });
  });

  describe('useMarkNotificationAsRead', () => {
    it('should return the correct function', () => {
      const { result } = renderHook(() => WitnessHooks.useMarkNotificationAsRead(), {
        wrapper,
      });
      expect(result.current).toEqual(markNotificationAsRead);
    });
  });

  describe('useIsEnabled', () => {
    it('should return the correct value', () => {
      const { result } = renderHook(() => WitnessHooks.useIsEnabled(), {
        wrapper,
      });
      expect(result.current).toEqual(true);
    });
  });
});
