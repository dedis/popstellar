import { render } from '@testing-library/react-native';
import React from 'react';
import { Provider } from 'react-redux';
import { combineReducers, createStore } from 'redux';

import MockNavigator from '__tests__/components/MockNavigator';
import FeatureContext from 'core/contexts/FeatureContext';
import {
  NotificationReactContext,
  NOTIFICATION_FEATURE_IDENTIFIER,
} from 'features/notification/interface/Configuration';
import { addNotification, notificationReducer } from 'features/notification/reducer';
import { WitnessNotificationTypeComponent } from 'features/witness/components';

import SingleNotificationScreen from '../SingleNotificationScreen';

const contextValue = {
  [NOTIFICATION_FEATURE_IDENTIFIER]: {
    notificationTypeComponents: [WitnessNotificationTypeComponent],
  } as NotificationReactContext,
};

// set up mock store
const mockStore = createStore(combineReducers({ ...notificationReducer }));
mockStore.dispatch(
  addNotification({ title: 'a notification', timestamp: 0, type: 'mock-notification' }),
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
