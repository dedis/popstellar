import { render } from '@testing-library/react-native';
import React from 'react';
import { Provider } from 'react-redux';
import { combineReducers, createStore } from 'redux';

import MockNavigator from '__tests__/components/MockNavigator';
import { mockLaoId } from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';
import { digitalCashReducer } from 'features/digital-cash/reducer';

import { mockDigitalCashContextValue } from '../../__tests__/utils';
import DigitalCashWallet from '../DigitalCashWallet';

const mockStore = createStore(combineReducers({ ...digitalCashReducer }));

describe('DigitalCashWallet', () => {
  it('renders correctly for organizers', () => {
    const { toJSON } = render(
      <Provider store={mockStore}>
        <FeatureContext.Provider value={mockDigitalCashContextValue(true)}>
          <MockNavigator component={DigitalCashWallet} params={{ laoId: mockLaoId }} />
        </FeatureContext.Provider>
      </Provider>,
    );
    expect(toJSON()).toMatchSnapshot();
  });

  it('renders correctly for non-organizers', () => {
    const { toJSON } = render(
      <Provider store={mockStore}>
        <FeatureContext.Provider value={mockDigitalCashContextValue(false)}>
          <MockNavigator component={DigitalCashWallet} params={{ laoId: mockLaoId }} />
        </FeatureContext.Provider>
      </Provider>,
    );
    expect(toJSON()).toMatchSnapshot();
  });
});
