import { render } from '@testing-library/react-native';
import React from 'react';
import { Provider } from 'react-redux';
import { combineReducers, createStore } from 'redux';

import MockNavigator from '__tests__/components/MockNavigator';
import { mockChannel, mockLao, mockReduxAction } from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';
import { HomeReactContext, HOME_FEATURE_IDENTIFIER } from 'features/home/interface';
import { LaoHooks } from 'features/lao/hooks';
import { connectToLao, laoReducer } from 'features/lao/reducer';

import ConnectNavigation from '../ConnectNavigation';

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
  } as HomeReactContext,
};

const mockStore = createStore(combineReducers(laoReducer));
mockStore.dispatch(connectToLao(mockLao.toState()));

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
