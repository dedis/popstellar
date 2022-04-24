import { describe } from '@jest/globals';
import { render } from '@testing-library/react-native';
import React from 'react';
import { Provider } from 'react-redux';
import { act } from 'react-test-renderer';
import { combineReducers, createStore } from 'redux';

import MockNavigator from '__tests__/components/MockNavigator';
import {
  mockKeyPair,
  mockLao,
  mockPopToken,
  wrapWithClearMockStorageReducer,
} from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';
import { keyPairReducer, setKeyPair } from 'core/keypair';
import { encodeLaoConnectionForQRCode } from 'features/connect/functions';
import { LaoReactContext, LAO_FEATURE_IDENTIFIER } from 'features/lao/interface';
import { LaoState } from 'features/lao/objects';
import { connectToLao, laoReducer } from 'features/lao/reducer';

import LaoProperties from '../LaoProperties';

// the qr code is rendered as a svg which results in a 2MB snapshot file
jest.mock('react-qr-code', () => 'qr code');

const contextValue = {
  [LAO_FEATURE_IDENTIFIER]: {
    EventList: () => null,
    encodeLaoConnectionForQRCode,
    laoNavigationScreens: [],
    organizerNavigationScreens: [],
  } as LaoReactContext,
};

// set up mock store
const mockStore = createStore(
  wrapWithClearMockStorageReducer(combineReducers({ ...laoReducer, ...keyPairReducer })),
);
beforeEach(() => {
  // clear data in the redux store
  mockStore.dispatch({ type: 'CLEAR_STORAGE' });
});

const LaoPropertiesScreen = () => <LaoProperties isInitiallyOpen />;

describe('LaoProperties', () => {
  it('renders correctly as organizer', () => {
    mockStore.dispatch(connectToLao(mockLao.toState()));
    mockStore.dispatch(setKeyPair(mockKeyPair.toState()));

    const component = render(
      <Provider store={mockStore}>
        <FeatureContext.Provider value={contextValue}>
          <MockNavigator component={LaoPropertiesScreen} />
        </FeatureContext.Provider>
      </Provider>,
    ).toJSON();
    expect(component).toMatchSnapshot();
  });

  it('renders correctly as witness', () => {
    mockStore.dispatch(
      connectToLao({
        ...mockLao.toState(),
        witnesses: [mockPopToken.publicKey.valueOf()],
      } as LaoState),
    );
    mockStore.dispatch(setKeyPair(mockPopToken.toState()));

    const component = render(
      <Provider store={mockStore}>
        <FeatureContext.Provider value={contextValue}>
          <MockNavigator component={LaoPropertiesScreen} />
        </FeatureContext.Provider>
      </Provider>,
    ).toJSON();
    expect(component).toMatchSnapshot();
  });

  it('renders correctly as attendee', () => {
    mockStore.dispatch(connectToLao(mockLao.toState()));
    mockStore.dispatch(setKeyPair(mockPopToken.toState()));

    const component = render(
      <Provider store={mockStore}>
        <FeatureContext.Provider value={contextValue}>
          <MockNavigator component={LaoPropertiesScreen} />
        </FeatureContext.Provider>
      </Provider>,
    ).toJSON();
    expect(component).toMatchSnapshot();
  });
});
