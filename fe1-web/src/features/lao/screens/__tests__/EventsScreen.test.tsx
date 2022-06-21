import { render } from '@testing-library/react-native';
import React from 'react';
import { Provider } from 'react-redux';
import { combineReducers, createStore } from 'redux';

import MockNavigator from '__tests__/components/MockNavigator';
import { mockLao, mockPopToken } from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';
import { keyPairReducer, setKeyPair } from 'core/keypair';
import { encodeLaoConnectionForQRCode } from 'features/home/functions';
import { LaoReactContext, LAO_FEATURE_IDENTIFIER } from 'features/lao/interface';
import { setCurrentLao, laoReducer } from 'features/lao/reducer';

import EventsScreen from '../EventsScreen';

const contextValue = {
  [LAO_FEATURE_IDENTIFIER]: {
    EventList: () => null,
    CreateEventButton: () => null,
    encodeLaoConnectionForQRCode,
    laoNavigationScreens: [],
    eventsNavigationScreens: [],
  } as LaoReactContext,
};

// set up mock store
const mockStore = createStore(combineReducers({ ...laoReducer, ...keyPairReducer }));
mockStore.dispatch(setCurrentLao(mockLao.toState()));
mockStore.dispatch(setKeyPair(mockPopToken.toState()));

describe('AttendeeScreen', () => {
  it('renders correctly', () => {
    const component = render(
      <Provider store={mockStore}>
        <FeatureContext.Provider value={contextValue}>
          <MockNavigator component={EventsScreen} />
        </FeatureContext.Provider>
      </Provider>,
    ).toJSON();
    expect(component).toMatchSnapshot();
  });
});
