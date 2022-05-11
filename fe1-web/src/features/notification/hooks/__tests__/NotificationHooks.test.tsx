import { describe } from '@jest/globals';
import { renderHook } from '@testing-library/react-hooks';
import React from 'react';
import { Provider } from 'react-redux';
import { combineReducers, createStore, Store } from 'redux';

import { mockLaoIdHash } from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';
import {
  NotificationReactContext,
  NOTIFICATION_FEATURE_IDENTIFIER,
} from 'features/notification/interface/Configuration';
import { notificationReducer } from 'features/notification/reducer';
import { WitnessNotificationType } from 'features/witness/components';

import { NotificationHooks } from '../NotificationHooks';

const contextValue = {
  [NOTIFICATION_FEATURE_IDENTIFIER]: {
    notificationTypes: [WitnessNotificationType],
    useCurrentLaoId: () => mockLaoIdHash,
  } as NotificationReactContext,
};

// setup mock store
const mockStore = createStore(combineReducers(notificationReducer));

const wrapper =
  (store: Store) =>
  ({ children }: { children: React.ReactChildren }) =>
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
