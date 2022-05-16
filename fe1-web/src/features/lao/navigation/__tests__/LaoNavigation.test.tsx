import { render } from '@testing-library/react-native';
import React from 'react';
import { Text } from 'react-native';
import { Provider } from 'react-redux';
import { combineReducers, createStore } from 'redux';

import MockNavigator from '__tests__/components/MockNavigator';
import { mockKeyPair, mockLao } from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';
import { keyPairReducer, setKeyPair } from 'core/keypair';
import { encodeLaoConnectionForQRCode } from 'features/home/functions';
import { LaoReactContext, LAO_FEATURE_IDENTIFIER } from 'features/lao/interface';
import { connectToLao, laoReducer } from 'features/lao/reducer';

import LaoNavigation from '../LaoNavigation';

const contextValue = {
  [LAO_FEATURE_IDENTIFIER]: {
    EventList: () => null,
    encodeLaoConnectionForQRCode,
    laoNavigationScreens: [
      { id: 'screen1', title: 'a title', order: 2, Component: () => <Text>first screen</Text> },
      { id: 'screen2', order: -2, Component: () => <Text>second screen</Text> },
    ],
    organizerNavigationScreens: [],
  } as LaoReactContext,
};

// set up mock store
const mockStore = createStore(combineReducers({ ...laoReducer, ...keyPairReducer }));
mockStore.dispatch(setKeyPair(mockKeyPair.toState()));
mockStore.dispatch(connectToLao(mockLao.toState()));

// react-navigation has a problem that makes this test always fail
// https://github.com/satya164/react-native-tab-view/issues/1104
describe.skip('LaoNavigation', () => {
  it('renders correctly', () => {
    const component = render(
      <Provider store={mockStore}>
        <FeatureContext.Provider value={contextValue}>
          <MockNavigator component={LaoNavigation} />
        </FeatureContext.Provider>
      </Provider>,
    ).toJSON();
    expect(component).toMatchSnapshot();
  });
});
