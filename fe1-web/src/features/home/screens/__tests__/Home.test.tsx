import { render } from '@testing-library/react-native';
import React from 'react';
import { Provider } from 'react-redux';
import { combineReducers, createStore } from 'redux';

import MockNavigator from '__tests__/components/MockNavigator';
import { mockChannel, mockLao, mockLaoIdHash, mockReduxAction } from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';
import { HomeReactContext, HOME_FEATURE_IDENTIFIER } from 'features/home/interface';
import { LaoList } from 'features/lao/components';
import { addLao, laoReducer, selectLaosList } from 'features/lao/reducer';

import Home from '../Home';

const contextValueEmpyList = {
  [HOME_FEATURE_IDENTIFIER]: {
    requestCreateLao: () => Promise.resolve(mockChannel),
    addLaoServerAddress: () => mockReduxAction,
    connectToTestLao: () => {},
    useLaoList: () => [],
    LaoList: () => null,
    homeNavigationScreens: [],
    getLaoChannel: () => mockChannel,
    useCurrentLaoId: () => mockLaoIdHash,
    useDisconnectFromLao: () => () => {},
    getLaoById: () => mockLao,
    resubscribeToLao: () => Promise.resolve(),
  } as HomeReactContext,
};

const contextValue = {
  [HOME_FEATURE_IDENTIFIER]: {
    requestCreateLao: () => Promise.resolve(mockChannel),
    addLaoServerAddress: () => mockReduxAction,
    connectToTestLao: () => {},
    useLaoList: () => [],
    LaoList,
    homeNavigationScreens: [],
    getLaoChannel: () => mockChannel,
    useCurrentLaoId: () => mockLaoIdHash,
    hasSeed: () => true,
    useDisconnectFromLao: () => () => {},
    getLaoById: () => mockLao,
    resubscribeToLao: () => Promise.resolve(),
  } as HomeReactContext,
};

describe('Home', () => {
  it('renders correctly with an empty list of LAOs', () => {
    const component = render(
      <FeatureContext.Provider value={contextValueEmpyList}>
        <MockNavigator component={Home} />
      </FeatureContext.Provider>,
    ).toJSON();
    expect(component).toMatchSnapshot();
  });

  it('renders correctly with an non-empty list of LAOs', () => {
    // setup mock store
    const mockStore = createStore(combineReducers(laoReducer));
    mockStore.dispatch(addLao(mockLao.toState()));
    // ensure the mock store contains the mock lao
    expect(selectLaosList(mockStore.getState())).toEqual([mockLao]);

    const component = render(
      <Provider store={mockStore}>
        <FeatureContext.Provider value={contextValue}>
          <MockNavigator component={Home} />
        </FeatureContext.Provider>
      </Provider>,
    ).toJSON();
    expect(component).toMatchSnapshot();
  });
});
