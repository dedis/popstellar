import { render } from '@testing-library/react-native';
import React from 'react';

import MockNavigator from '__tests__/components/MockNavigator';
import { mockChannel, mockLao, mockLaoIdHash, mockReduxAction } from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';
import { HomeFeature, HomeReactContext, HOME_FEATURE_IDENTIFIER } from 'features/home/interface';
import { Home } from 'features/home/screens';

import HomeNavigation from '../HomeNavigation';

const contextValue = {
  [HOME_FEATURE_IDENTIFIER]: {
    requestCreateLao: () => Promise.resolve(mockChannel),
    addLaoServerAddress: () => mockReduxAction,
    connectToTestLao: () => {},
    useLaoList: () => [],
    LaoList: () => null,
    homeNavigationScreens: [
      { id: 'home' as HomeFeature.HomeScreen['id'], title: 'Home', Component: Home },
      { id: 'home2' as HomeFeature.HomeScreen['id'], Component: Home },
    ],
    getLaoChannel: () => mockChannel,
    useCurrentLaoId: () => mockLaoIdHash,
    useDisconnectFromLao: () => () => {},
    getLaoById: () => mockLao,
    resubscribeToLao: () => Promise.resolve(),
    forgetSeed: () => {},
  } as HomeReactContext,
};

describe('HomeNavigation', () => {
  it('renders correctly', () => {
    const component = render(
      <FeatureContext.Provider value={contextValue}>
        <MockNavigator component={HomeNavigation} />
      </FeatureContext.Provider>,
    ).toJSON();
    expect(component).toMatchSnapshot();
  });
});
