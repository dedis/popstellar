import { configureStore } from '@reduxjs/toolkit';
import { render } from '@testing-library/react-native';
import React from 'react';
import { Provider } from 'react-redux';
import { combineReducers } from 'redux';

import MockNavigator from '__tests__/components/MockNavigator';
import { mockChannel, mockLao, mockReduxAction } from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';
import { HOME_FEATURE_IDENTIFIER, HomeReactContext } from 'features/home/interface';

import Launch from '../Launch';

const contextValue = {
  [HOME_FEATURE_IDENTIFIER]: {
    requestCreateLao: () => Promise.resolve('a channel'),
    addLaoServerAddress: () => mockReduxAction,
    connectToTestLao: () => {},
    useLaoList: () => [],
    LaoList: () => null,
    homeNavigationScreens: [],
    getLaoChannel: () => mockChannel,
    useConnectedToLao: () => true,
    useDisconnectFromLao: () => () => {},
    getLaoById: () => mockLao,
    resubscribeToLao: () => Promise.resolve(),
    forgetSeed: () => {},
  } as HomeReactContext,
};

const mockStore = configureStore({ reducer: combineReducers({}) });

describe('Launch', () => {
  it('renders correctly', () => {
    const component = render(
      <Provider store={mockStore}>
        <FeatureContext.Provider value={contextValue}>
          <MockNavigator component={Launch} />
        </FeatureContext.Provider>
      </Provider>,
    ).toJSON();
    expect(component).toMatchSnapshot();
  });
});
