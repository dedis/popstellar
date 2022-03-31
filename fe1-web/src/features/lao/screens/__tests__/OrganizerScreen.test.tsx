import { render } from '@testing-library/react-native';
import React from 'react';
import { Provider } from 'react-redux';
import { combineReducers, createStore } from 'redux';

import MockNavigator from '__tests__/components/MockNavigator';
import { mockLao } from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';
import { encodeLaoConnectionForQRCode } from 'features/connect/functions';
import { LaoReactContext, LAO_FEATURE_IDENTIFIER } from 'features/lao/interface';
import { connectToLao, laoReducer } from 'features/lao/reducer';

import OrganizerScreen from '../OrganizerScreen';

const contextValue = {
  [LAO_FEATURE_IDENTIFIER]: {
    EventList: () => null,
    encodeLaoConnectionForQRCode,
    laoNavigationScreens: [],
    organizerNavigationScreens: [],
  } as LaoReactContext,
};

// set up mock store
const mockStore = createStore(combineReducers({ ...laoReducer }));
mockStore.dispatch(connectToLao(mockLao.toState()));

describe('OrganizerScreen', () => {
  it('renders correctly', () => {
    const component = render(
      <Provider store={mockStore}>
        <FeatureContext.Provider value={contextValue}>
          <MockNavigator component={OrganizerScreen} />
        </FeatureContext.Provider>
      </Provider>,
    ).toJSON();
    expect(component).toMatchSnapshot();
  });
});
