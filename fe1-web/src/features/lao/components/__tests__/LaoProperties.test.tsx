import { describe } from '@jest/globals';
import { configureStore } from '@reduxjs/toolkit';
import { render } from '@testing-library/react-native';
import React from 'react';
import { Provider } from 'react-redux';
import { combineReducers } from 'redux';

import MockNavigator from '__tests__/components/MockNavigator';
import { mockKeyPair, mockLao, mockPopToken } from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';
import { keyPairReducer, setKeyPair } from 'core/keypair';
import { encodeLaoConnectionForQRCode } from 'features/home/functions';
import { LAO_FEATURE_IDENTIFIER, LaoReactContext } from 'features/lao/interface';
import { LaoState } from 'features/lao/objects';
import { laoReducer, setCurrentLao } from 'features/lao/reducer';

import LaoProperties from '../LaoProperties';

// the qr code is rendered as a svg which results in a 2MB snapshot file
jest.mock('react-qr-code', () => 'qr code');

const contextValue = {
  [LAO_FEATURE_IDENTIFIER]: {
    EventList: () => null,
    CreateEventButton: () => null,
    encodeLaoConnectionForQRCode,
    laoNavigationScreens: [],
    eventsNavigationScreens: [],
  } as LaoReactContext,
};

const LaoPropertiesScreen = () => <LaoProperties />;

describe('LaoProperties', () => {
  it('renders correctly as organizer', () => {
    const mockStore = configureStore({
      reducer: combineReducers({
        ...laoReducer,
        ...keyPairReducer,
      }),
    });
    mockStore.dispatch(setCurrentLao(mockLao.toState()));
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
    const mockStore = configureStore({
      reducer: combineReducers({
        ...laoReducer,
        ...keyPairReducer,
      }),
    });
    mockStore.dispatch(
      setCurrentLao({
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
    const mockStore = configureStore({
      reducer: combineReducers({
        ...laoReducer,
        ...keyPairReducer,
      }),
    });
    mockStore.dispatch(setCurrentLao(mockLao.toState()));
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
