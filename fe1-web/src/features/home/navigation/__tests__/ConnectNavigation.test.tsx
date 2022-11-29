import { configureStore } from '@reduxjs/toolkit';
import { render } from '@testing-library/react-native';
import React from 'react';
import { Provider } from 'react-redux';
import { combineReducers } from 'redux';

import MockNavigator from '__tests__/components/MockNavigator';
import { mockChannel, mockLao, mockReduxAction } from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';
import { HOME_FEATURE_IDENTIFIER, HomeReactContext } from 'features/home/interface';
import { laoReducer, setCurrentLao } from 'features/lao/reducer';

import ConnectNavigation from '../ConnectNavigation';

const contextValue = {
  [HOME_FEATURE_IDENTIFIER]: {
    addLaoServerAddress: () => mockReduxAction,
    useConnectedToLao: () => true,
    getLaoChannel: () => mockChannel,
    requestCreateLao: () => Promise.resolve(mockChannel),
    connectToTestLao: () => {},
    useLaoList: () => [],
    LaoList: () => null,
    homeNavigationScreens: [],
    useDisconnectFromLao: () => () => {},
    getLaoById: () => mockLao,
    resubscribeToLao: () => Promise.resolve(),
    forgetSeed: () => {},
  } as HomeReactContext,
};

const mockStore = configureStore({ reducer: combineReducers(laoReducer) });
mockStore.dispatch(setCurrentLao({ lao: mockLao.toState() }));

describe('ConnectNavigation', () => {
  it('renders correctly', () => {
    const component = render(
      <Provider store={mockStore}>
        <FeatureContext.Provider value={contextValue}>
          <MockNavigator component={ConnectNavigation} />
        </FeatureContext.Provider>
      </Provider>,
    ).toJSON();
    expect(component).toMatchSnapshot();
  });
});
