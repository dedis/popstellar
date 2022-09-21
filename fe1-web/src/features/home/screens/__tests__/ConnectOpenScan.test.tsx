import { useNavigation } from '@react-navigation/core';
import { configureStore } from '@reduxjs/toolkit';
import { act, render, waitFor } from '@testing-library/react-native';
// @ts-ignore
import { fireScan as fakeQrReaderScan } from 'expo-camera';
import React from 'react';
import { Provider } from 'react-redux';
import { combineReducers } from 'redux';

import MockNavigator from '__tests__/components/MockNavigator';
import {
  mockAddress,
  mockChannel,
  mockLao,
  mockLaoId,
  mockLaoIdHash,
  mockReduxAction,
} from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';
import { subscribeToChannel } from 'core/network';
import { HOME_FEATURE_IDENTIFIER, HomeReactContext } from 'features/home/interface';
import { ConnectToLao } from 'features/home/objects';
import { getLaoChannel, resubscribeToLao } from 'features/lao/functions';
import { LaoHooks } from 'features/lao/hooks';
import { laoReducer, setCurrentLao } from 'features/lao/reducer';

import ConnectOpenScan from '../ConnectOpenScan';

jest.mock('expo-camera');
jest.mock('websocket');

jest.mock('@react-navigation/core', () => {
  const actualNavigation = jest.requireActual('@react-navigation/core');

  const mockNavigate = jest.fn();
  const mockAddListener = jest.fn();

  return {
    ...actualNavigation,
    useNavigation: () => ({
      navigate: mockNavigate,
      addListener: mockAddListener,
    }),
  };
});

// Is mocked
// eslint-disable-next-line react-hooks/rules-of-hooks
const { navigate: mockNavigate, addListener } = useNavigation();

const didFocus = () =>
  // call focus event listener
  (addListener as jest.Mock).mock.calls
    .filter(([eventName]) => eventName === 'focus')
    .forEach((args) => args[1]());

const mockConnection = 0;

jest.mock('core/network', () => {
  return {
    ...jest.requireActual('core/network'),
    subscribeToChannel: jest.fn(() => Promise.resolve()),
    connect: jest.fn(() => Promise.resolve(mockConnection)),
  };
});

beforeEach(jest.clearAllMocks);

const contextValue = {
  [HOME_FEATURE_IDENTIFIER]: {
    addLaoServerAddress: () => mockReduxAction,
    useCurrentLaoId: LaoHooks.useCurrentLaoId,
    getLaoChannel: () => mockChannel,
    LaoList: () => null,
    connectToTestLao: () => {},
    homeNavigationScreens: [],
    requestCreateLao: () => Promise.resolve(mockChannel),
    useLaoList: () => [],
    useDisconnectFromLao: () => () => {},
    getLaoById: () => mockLao,
    resubscribeToLao,
  } as HomeReactContext,
};

const mockStore = configureStore({ reducer: combineReducers(laoReducer) });
mockStore.dispatch(setCurrentLao(mockLao.toState()));

describe('ConnectOpenScan', () => {
  it('renders correctly', () => {
    const component = render(
      <Provider store={mockStore}>
        <FeatureContext.Provider value={contextValue}>
          <MockNavigator component={ConnectOpenScan} />
        </FeatureContext.Provider>
      </Provider>,
    ).toJSON();
    expect(component).toMatchSnapshot();
  });

  it('can connect to a lao', async () => {
    render(
      <Provider store={mockStore}>
        <FeatureContext.Provider value={contextValue}>
          <MockNavigator component={ConnectOpenScan} />
        </FeatureContext.Provider>
      </Provider>,
    );

    act(didFocus);

    act(() =>
      fakeQrReaderScan(
        new ConnectToLao({
          lao: mockLaoId,
          servers: [mockAddress],
        }).toJson(),
      ),
    );

    await waitFor(() => {
      expect(subscribeToChannel).toHaveBeenCalledWith(
        mockLaoIdHash,
        expect.anything(),
        getLaoChannel(mockLaoId),
        expect.anything(),
      );

      expect(mockNavigate).toHaveBeenCalledTimes(1);
    });
  });
});
