import { render } from '@testing-library/react-native';
import React from 'react';
import { Provider } from 'react-redux';
import { combineReducers, createStore } from 'redux';

import { addNotification, notificationReducer } from 'features/notification/reducer';

import NotificationBadge from '../NotificationBadge';

// set up mock store

describe('NotificationScreen', () => {
  it('renders correctly for an empty store', () => {
    const mockStore = createStore(combineReducers({ ...notificationReducer }));

    const component = render(
      <Provider store={mockStore}>
        <NotificationBadge />
      </Provider>,
    ).toJSON();
    expect(component).toMatchSnapshot();
  });

  it('renders correctly for a non-empty store', () => {
    const mockStore = createStore(combineReducers({ ...notificationReducer }));
    mockStore.dispatch(
      addNotification({ title: 'a notification', timestamp: 0, type: 'mock-notification' }),
    );

    const component = render(
      <Provider store={mockStore}>
        <NotificationBadge />
      </Provider>,
    ).toJSON();
    expect(component).toMatchSnapshot();
  });
});
