import { configureStore } from '@reduxjs/toolkit';
import { render } from '@testing-library/react-native';
import React from 'react';
import { Text } from 'react-native';
import { Provider } from 'react-redux';
import { combineReducers } from 'redux';

import MockNavigator from '__tests__/components/MockNavigator';
import { mockKeyPair, mockLao } from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';
import { keyPairReducer, setKeyPair } from 'core/keypair';
import { encodeLaoConnectionForQRCode } from 'features/home/functions';
import { LAO_FEATURE_IDENTIFIER, LaoFeature, LaoReactContext } from 'features/lao/interface';
import { laoReducer, setCurrentLao } from 'features/lao/reducer';

import EventsNavigation from '../EventsNavigation';

const contextValue = {
  [LAO_FEATURE_IDENTIFIER]: {
    EventList: () => null,
    CreateEventButton: () => null,
    encodeLaoConnectionForQRCode,
    laoNavigationScreens: [],
    eventsNavigationScreens: [
      {
        id: 'screen1' as LaoFeature.LaoEventScreen['id'],
        title: 'a title',
        Component: () => <Text>first screen</Text>,
      },
      {
        id: 'screen2' as LaoFeature.LaoEventScreen['id'],
        Component: () => <Text>second screen</Text>,
      },
    ],
  } as LaoReactContext,
};

// set up mock store
const mockStore = configureStore({
  reducer: combineReducers({
    ...laoReducer,
    ...keyPairReducer,
  }),
});
mockStore.dispatch(setCurrentLao(mockLao));
mockStore.dispatch(setKeyPair(mockKeyPair.toState()));

describe('OrganizerNavigation', () => {
  it('renders correctly', () => {
    const component = render(
      <Provider store={mockStore}>
        <FeatureContext.Provider value={contextValue}>
          <MockNavigator component={EventsNavigation} />
        </FeatureContext.Provider>
      </Provider>,
    ).toJSON();
    expect(component).toMatchSnapshot();
  });
});
