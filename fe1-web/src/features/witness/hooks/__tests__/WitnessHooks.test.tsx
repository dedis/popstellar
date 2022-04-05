import { describe } from '@jest/globals';
import { renderHook } from '@testing-library/react-hooks';
import React from 'react';

import FeatureContext from 'core/contexts/FeatureContext';
import { WintessReactContext, WITNESS_FEATURE_IDENTIFIER } from 'features/witness/interface';

import { WitnessHooks } from '../WitnessHooks';

const addNotification = jest.fn();
const discardNotifications = jest.fn();
const markNotificationAsRead = jest.fn();

const contextValue = {
  [WITNESS_FEATURE_IDENTIFIER]: {
    addNotification,
    discardNotifications,
    markNotificationAsRead,
  } as WintessReactContext,
};

// setup mock store

const wrapper = ({ children }: { children: React.ReactChildren }) => (
  <FeatureContext.Provider value={contextValue}>{children}</FeatureContext.Provider>
);

describe('WitnessHooks', () => {
  describe('WitnessHooks.useDiscardNotifications', () => {
    it('should return the correct function', () => {
      const { result } = renderHook(() => WitnessHooks.useDiscardNotifications(), {
        wrapper,
      });
      expect(result.current).toEqual(discardNotifications);
    });
  });

  describe('WitnessHooks.useMarkNotificationAsRead', () => {
    it('should return the correct function', () => {
      const { result } = renderHook(() => WitnessHooks.useMarkNotificationAsRead(), {
        wrapper,
      });
      expect(result.current).toEqual(markNotificationAsRead);
    });
  });
});
