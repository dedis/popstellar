import { configureStore } from '@reduxjs/toolkit';
import { render } from '@testing-library/react-native';
import React from 'react';
import { Provider } from 'react-redux';
import { combineReducers } from 'redux';

import { mockLaoId } from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';
import {
  NOTIFICATION_FEATURE_IDENTIFIER,
  NotificationReactContext,
} from 'features/notification/interface/Configuration';
import { addNotification, notificationReducer } from 'features/notification/reducer';
import { WitnessNotificationType } from 'features/witness/components';

import NotificationBadge from '../NotificationBadge';

const contextValue = {
  [NOTIFICATION_FEATURE_IDENTIFIER]: {
    useCurrentLaoId: () => mockLaoId,
    notificationTypes: [WitnessNotificationType],
  } as NotificationReactContext,
};

describe('NotificationScreen', () => {
  it('renders correctly for an empty store', () => {
    const mockStore = configureStore({ reducer: combineReducers({ ...notificationReducer }) });

    const component = render(
      <Provider store={mockStore}>
        <FeatureContext.Provider value={contextValue}>
          <NotificationBadge />
        </FeatureContext.Provider>
      </Provider>,
    ).toJSON();
    expect(component).toMatchSnapshot();
  });

  it('renders correctly for a non-empty store', () => {
    const mockStore = configureStore({ reducer: combineReducers({ ...notificationReducer }) });
    mockStore.dispatch(
      addNotification({
        laoId: mockLaoId.toState(),
        title: 'a notification',
        timestamp: 0,
        type: 'mock-notification',
      }),
    );

    const component = render(
      <Provider store={mockStore}>
        <FeatureContext.Provider value={contextValue}>
          <NotificationBadge />
        </FeatureContext.Provider>
      </Provider>,
    ).toJSON();
    expect(component).toMatchSnapshot();
  });
});
