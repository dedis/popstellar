import { configureStore } from '@reduxjs/toolkit';
import { render } from '@testing-library/react-native';
import React from 'react';
import { Provider } from 'react-redux';
import { combineReducers } from 'redux';

import MockNavigator from '__tests__/components/MockNavigator';
import { mockLaoId, mockLaoIdHash } from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';
import {
  NOTIFICATION_FEATURE_IDENTIFIER,
  NotificationReactContext,
} from 'features/notification/interface/Configuration';
import { addNotification, notificationReducer } from 'features/notification/reducer';
import { WitnessNotificationType } from 'features/witness/components';

import SingleNotificationScreen from '../SingleNotificationScreen';

const contextValue = {
  [NOTIFICATION_FEATURE_IDENTIFIER]: {
    useAssertCurrentLaoId: () => mockLaoIdHash,
    notificationTypes: [WitnessNotificationType],
  } as NotificationReactContext,
};

// set up mock store
const mockStore = configureStore({ reducer: combineReducers({ ...notificationReducer }) });
mockStore.dispatch(
  addNotification({
    laoId: mockLaoId,
    title: 'a notification',
    timestamp: 0,
    type: 'mock-notification',
  }),
);

describe('SingleNotificationScreen', () => {
  it('renders correctly', () => {
    const component = render(
      <Provider store={mockStore}>
        <FeatureContext.Provider value={contextValue}>
          <MockNavigator component={SingleNotificationScreen} />
        </FeatureContext.Provider>
      </Provider>,
    ).toJSON();
    expect(component).toMatchSnapshot();
  });
});
