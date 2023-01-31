import { describe } from '@jest/globals';
import { configureStore } from '@reduxjs/toolkit';
import { render } from '@testing-library/react-native';
import React from 'react';
import { Provider } from 'react-redux';
import { combineReducers } from 'redux';

import MockNavigator from '__tests__/components/MockNavigator';
import { mockKeyPair, mockLao, mockPopToken } from '__tests__/utils';
import { keyPairReducer, setKeyPair } from 'core/keypair';
import { Lao } from 'features/lao/objects';
import { addLao, laoReducer } from 'features/lao/reducer';

import LaoItem from '../LaoItem';

describe('LaoItem', () => {
  it('renders correctly as organizer', () => {
    const mockStore = configureStore({
      reducer: combineReducers({
        ...laoReducer,
        ...keyPairReducer,
      }),
    });
    mockStore.dispatch(addLao(mockLao));
    mockStore.dispatch(setKeyPair(mockKeyPair.toState()));

    const component = render(
      <Provider store={mockStore}>
        <MockNavigator
          component={() => <LaoItem lao={mockLao} isFirstItem={false} isLastItem={false} />}
        />
      </Provider>,
    ).toJSON();
    expect(component).toMatchSnapshot();
  });

  it('renders correctly as witness', () => {
    const mockStore = configureStore({
      reducer: combineReducers({
        ...laoReducer,
        ...keyPairReducer,
      }),
    });
    mockStore.dispatch(
      addLao(
        Lao.fromState({
          ...mockLao.toState(),
          witnesses: [mockPopToken.publicKey.valueOf()],
        }),
      ),
    );
    mockStore.dispatch(setKeyPair(mockPopToken.toState()));

    const component = render(
      <Provider store={mockStore}>
        <MockNavigator
          component={() => <LaoItem lao={mockLao} isFirstItem={false} isLastItem={false} />}
        />
      </Provider>,
    ).toJSON();
    expect(component).toMatchSnapshot();
  });

  it('renders correctly as attendee', () => {
    const mockStore = configureStore({
      reducer: combineReducers({
        ...laoReducer,
        ...keyPairReducer,
      }),
    });
    mockStore.dispatch(addLao(mockLao));
    mockStore.dispatch(setKeyPair(mockPopToken.toState()));

    const component = render(
      <Provider store={mockStore}>
        <MockNavigator
          component={() => <LaoItem lao={mockLao} isFirstItem={false} isLastItem={false} />}
        />
      </Provider>,
    ).toJSON();
    expect(component).toMatchSnapshot();
  });
});
