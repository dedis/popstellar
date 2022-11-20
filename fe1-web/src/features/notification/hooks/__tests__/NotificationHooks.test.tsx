import { describe } from '@jest/globals';
import { configureStore } from '@reduxjs/toolkit';
import { renderHook } from '@testing-library/react-hooks';
import React from 'react';
import { Provider } from 'react-redux';
import { combineReducers, Store } from 'redux';

import { mockLaoIdHash } from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';
import {
  NOTIFICATION_FEATURE_IDENTIFIER,
  NotificationReactContext,
} from 'features/notification/interface/Configuration';
import { notificationReducer } from 'features/notification/reducer';
import { WitnessNotificationType } from 'features/witness/components';

import { NotificationHooks } from '../NotificationHooks';

const contextValue = {
  [NOTIFICATION_FEATURE_IDENTIFIER]: {
    useAssertCurrentLaoId: () => mockLaoIdHash,
    notificationTypes: [WitnessNotificationType],
  } as NotificationReactContext,
};

// setup mock store
const mockStore = configureStore({ reducer: combineReducers(notificationReducer) });

const wrapper =
  (store: Store) =>
  ({ children }: { children: React.ReactNode }) =>
    (
      <Provider store={store}>
        <FeatureContext.Provider value={contextValue}>{children}</FeatureContext.Provider>
      </Provider>
    );

describe('NotificationHooks', () => {
  describe('useNotificationTypes', () => {
    it('should return the notification types', () => {
      const { result } = renderHook(() => NotificationHooks.useNotificationTypes(), {
        wrapper: wrapper(mockStore),
      });
      expect(result.current).toEqual([WitnessNotificationType]);
    });
  });
});
