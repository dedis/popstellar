import { render } from '@testing-library/react-native';
import React from 'react';
import { Provider } from 'react-redux';
import { combineReducers, createStore } from 'redux';

import MockNavigator from '__tests__/components/MockNavigator';
import { mockLaoId, mockLaoIdHash } from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';
import {
  NotificationReactContext,
  NOTIFICATION_FEATURE_IDENTIFIER,
} from 'features/notification/interface/Configuration';
import {
  addNotification,
  markNotificationAsRead,
  notificationReducer,
} from 'features/notification/reducer';
import { WitnessNotificationType } from 'features/witness/components';
import { WitnessFeature } from 'features/witness/interface';

import NotificationScreen from '../NotificationScreen';

const contextValue = {
  [NOTIFICATION_FEATURE_IDENTIFIER]: {
    useCurrentLaoId: () => mockLaoIdHash,
    notificationTypes: [WitnessNotificationType],
  } as NotificationReactContext,
};

// set up mock store
const mockStore = createStore(combineReducers({ ...notificationReducer }));
mockStore.dispatch(
  addNotification({
    laoId: mockLaoId,
    title: 'a notification',
    timestamp: 0,
    type: WitnessFeature.NotificationTypes.MESSAGE_TO_WITNESS,
  }),
);
mockStore.dispatch(
  addNotification({
    laoId: mockLaoId,
    title: 'another notification',
    timestamp: 1,
    type: WitnessFeature.NotificationTypes.MESSAGE_TO_WITNESS,
  }),
);
mockStore.dispatch(markNotificationAsRead({ laoId: mockLaoId, notificationId: 0 }));

describe('NotificationScreen', () => {
  it('renders correctly', () => {
    const component = render(
      <Provider store={mockStore}>
        <FeatureContext.Provider value={contextValue}>
          <MockNavigator component={NotificationScreen} />
        </FeatureContext.Provider>
      </Provider>,
    ).toJSON();
    expect(component).toMatchSnapshot();
  });
});
