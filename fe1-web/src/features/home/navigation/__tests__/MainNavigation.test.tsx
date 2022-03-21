import { NavigationContainer } from '@react-navigation/native';
import { render } from '@testing-library/react-native';
import React from 'react';

import MockNavigator from '__tests__/components/MockNavigator';
import { mockReduxAction } from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';
import { HomeReactContext, HOME_FEATURE_IDENTIFIER } from 'features/home/interface';
import { Home } from 'features/home/screens';

import MainNavigation from '../MainNavigation';

const contextValue = {
  [HOME_FEATURE_IDENTIFIER]: {
    createLao: () => Promise.resolve('a channel'),
    addLaoServerAddress: () => mockReduxAction,
    connectToTestLao: () => {},
    useLaoList: () => [],
    LaoList: () => null,
    mainNavigationScreens: [
      { id: 'home', title: 'Home', order: 0, Component: Home },
      { id: 'home2', order: -3, Component: Home },
    ],
  } as HomeReactContext,
};

// react-navigation has a problem that makes this test always fail
// https://github.com/satya164/react-native-tab-view/issues/1104
describe.skip('MainNavigation', () => {
  it('renders correctly', () => {
    const component = render(
      <FeatureContext.Provider value={contextValue}>
        <MockNavigator component={MainNavigation} />
      </FeatureContext.Provider>,
    ).toJSON();
    expect(component).toMatchSnapshot();
  });
});
