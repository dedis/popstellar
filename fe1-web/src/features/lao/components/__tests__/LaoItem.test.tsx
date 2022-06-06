import { describe } from '@jest/globals';
import { render } from '@testing-library/react-native';
import React from 'react';
import { Provider } from 'react-redux';
import { combineReducers, createStore } from 'redux';

import MockNavigator from '__tests__/components/MockNavigator';
import { mockKeyPair, mockLao, mockPopToken } from '__tests__/utils';
import { keyPairReducer, setKeyPair } from 'core/keypair';
import { addLao, laoReducer } from 'features/lao/reducer';

import LaoItem from '../LaoItem';

describe('LaoItem', () => {
  it('renders correctly as organizer', () => {
    const mockStore = createStore(combineReducers({ ...laoReducer, ...keyPairReducer }));
    mockStore.dispatch(addLao(mockLao.toState()));
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
    const mockStore = createStore(combineReducers({ ...laoReducer, ...keyPairReducer }));
    mockStore.dispatch(
      addLao({ ...mockLao.toState(), witnesses: [mockPopToken.publicKey.valueOf()] }),
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
    const mockStore = createStore(combineReducers({ ...laoReducer, ...keyPairReducer }));
    mockStore.dispatch(addLao(mockLao.toState()));
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
