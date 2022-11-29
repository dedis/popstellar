import { configureStore } from '@reduxjs/toolkit';
import { render } from '@testing-library/react-native';
import React from 'react';
import { Provider } from 'react-redux';
import { combineReducers } from 'redux';

import MockNavigator from '__tests__/components/MockNavigator';
import { mockLao } from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';
import { encodeLaoConnectionForQRCode } from 'features/home/functions';
import { LAO_FEATURE_IDENTIFIER, LaoReactContext } from 'features/lao/interface';
import { laoReducer, setCurrentLao } from 'features/lao/reducer';

import IdentityScreen from '../IdentityScreen';

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
    laoNavigationScreens: [],
    eventsNavigationScreens: [],
  } as LaoReactContext,
};

// set up mock store
const mockStore = configureStore({ reducer: combineReducers({ ...laoReducer }) });
mockStore.dispatch(setCurrentLao({ lao: mockLao.toState() }));

describe('LaoHomeScreen', () => {
  it('renders correctly', () => {
    const component = render(
      <Provider store={mockStore}>
        <FeatureContext.Provider value={contextValue}>
          <MockNavigator component={IdentityScreen} />
        </FeatureContext.Provider>
      </Provider>,
    ).toJSON();
    expect(component).toMatchSnapshot();
  });
});
