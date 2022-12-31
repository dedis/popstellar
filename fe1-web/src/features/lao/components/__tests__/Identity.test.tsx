import { configureStore } from '@reduxjs/toolkit';
import { render } from '@testing-library/react-native';
import React from 'react';
import { Provider } from 'react-redux';
import { combineReducers } from 'redux';

import MockNavigator from '__tests__/components/MockNavigator';
import { configureTestFeatures, mockLao } from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';
import { dispatch } from 'core/redux';
import { encodeLaoConnectionForQRCode } from 'features/home/functions';
import { LAO_FEATURE_IDENTIFIER, LaoReactContext } from 'features/lao/interface';
import { laoReducer, setCurrentLao } from 'features/lao/reducer';

import Identity from '../Identity';

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
const mockStore = configureStore({ reducer: combineReducers({ ...laoReducer }) });
mockStore.dispatch(setCurrentLao(mockLao));

// KeyPairStore always accesses the global store, hence the full store is required
beforeAll(() => {
  configureTestFeatures();
});

beforeEach(() => {
  // clear data in the redux store
  dispatch({
    type: 'CLEAR_STORAGE',
    value: {},
  });
});

describe('Identity', () => {
  it('renders correctly', () => {
    const component = render(
      <Provider store={mockStore}>
        <FeatureContext.Provider value={contextValue}>
          <MockNavigator component={Identity} />
        </FeatureContext.Provider>
      </Provider>,
    ).toJSON();
    expect(component).toMatchSnapshot();
  });
});
