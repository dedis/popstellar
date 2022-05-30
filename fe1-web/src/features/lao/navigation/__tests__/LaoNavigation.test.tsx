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
import { LaoFeature, LaoReactContext, LAO_FEATURE_IDENTIFIER } from 'features/lao/interface';
import { connectToLao, laoReducer } from 'features/lao/reducer';

import LaoNavigation from '../LaoNavigation';

jest.mock('react-qr-code', () => {
  const MockQrCode = (props: any) => `[QrCode ${JSON.stringify(props)}]`;
  return {
    __esModule: true,
    default: MockQrCode,
  };
});

const contextValue = {
  [LAO_FEATURE_IDENTIFIER]: {
    EventList: () => null,
    CreateEventButton: () => null,
    encodeLaoConnectionForQRCode,
    laoNavigationScreens: [
      {
        id: 'screen1' as LaoFeature.LaoScreen['id'],
        title: 'a title',
        order: 2,
        Component: () => <Text>first screen</Text>,
      },
      {
        id: 'screen2' as LaoFeature.LaoScreen['id'],
        order: -2,
        Component: () => <Text>second screen</Text>,
      },
    ],
    eventsNavigationScreens: [],
  } as LaoReactContext,
};

// set up mock store
const mockStore = createStore(combineReducers({ ...laoReducer, ...keyPairReducer }));
mockStore.dispatch(setKeyPair(mockKeyPair.toState()));
mockStore.dispatch(connectToLao(mockLao.toState()));

describe('LaoNavigation', () => {
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
