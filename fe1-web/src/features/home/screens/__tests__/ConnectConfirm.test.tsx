import { useNavigation } from '@react-navigation/core';
import { configureStore } from '@reduxjs/toolkit';
import { fireEvent, render, waitFor } from '@testing-library/react-native';
import React from 'react';
import { Provider } from 'react-redux';
import { combineReducers } from 'redux';

import MockNavigator from '__tests__/components/MockNavigator';
import {
  mockAddress,
  mockChannel,
  mockLao,
  serializedMockLaoId,
  mockLaoId,
  mockReduxAction,
} from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';
import { subscribeToChannel } from 'core/network';
import { HOME_FEATURE_IDENTIFIER, HomeReactContext } from 'features/home/interface';
import { getLaoChannel, resubscribeToLao } from 'features/lao/functions';
import { LaoHooks } from 'features/lao/hooks';
import { laoReducer, setCurrentLao } from 'features/lao/reducer';

import ConnectConfirm from '../ConnectConfirm';

const contextValue = {
  [HOME_FEATURE_IDENTIFIER]: {
    addLaoServerAddress: () => mockReduxAction,
    useCurrentLaoId: LaoHooks.useCurrentLaoId,
    getLaoChannel: () => mockChannel,
    requestCreateLao: () => Promise.resolve(mockChannel),
    connectToTestLao: () => {},
    useLaoList: () => [],
    LaoList: () => null,
    homeNavigationScreens: [],
    useDisconnectFromLao: () => () => {},
    getLaoById: () => mockLao,
    resubscribeToLao,
    forgetSeed: () => {},
  } as HomeReactContext,
};

jest.mock('websocket');

jest.mock('@react-navigation/core', () => {
  const actualNavigation = jest.requireActual('@react-navigation/core');

  const mockNavigate = jest.fn();

  return {
    ...actualNavigation,
    useNavigation: () => ({
      navigate: mockNavigate,
    }),
  };
});

// Is mocked
// eslint-disable-next-line react-hooks/rules-of-hooks
const mockNavigate = useNavigation().navigate;

const mockConnection = 0;

jest.mock('core/network', () => {
  return {
    ...jest.requireActual('core/network'),
    subscribeToChannel: jest.fn(() => Promise.resolve()),
    connect: jest.fn(() => Promise.resolve(mockConnection)),
  };
});

beforeEach(jest.clearAllMocks);

const mockStore = configureStore({ reducer: combineReducers(laoReducer) });
mockStore.dispatch(setCurrentLao(mockLao));

describe('ConnectNavigation', () => {
  it('renders correctly', () => {
    const component = render(
      <Provider store={mockStore}>
        <FeatureContext.Provider value={contextValue}>
          <MockNavigator component={ConnectConfirm} />
        </FeatureContext.Provider>
      </Provider>,
    ).toJSON();
    expect(component).toMatchSnapshot();
  });

  it('can connect to a lao', async () => {
    const { getByTestId } = render(
      <Provider store={mockStore}>
        <FeatureContext.Provider value={contextValue}>
          <MockNavigator
            component={ConnectConfirm}
            params={{
              laoId: serializedMockLaoId,
              serverUrl: mockAddress,
            }}
          />
        </FeatureContext.Provider>
      </Provider>,
    );

    fireEvent.press(getByTestId('connect-button'));

    await waitFor(() => {
      expect(subscribeToChannel).toHaveBeenCalledWith(
        mockLaoId,
        expect.anything(),
        getLaoChannel(mockLaoId),
        expect.anything(),
      );

      expect(mockNavigate).toHaveBeenCalledTimes(1);
    });
  });
});
