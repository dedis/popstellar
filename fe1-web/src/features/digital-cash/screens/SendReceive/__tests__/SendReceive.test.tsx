import { render } from '@testing-library/react-native';
import React from 'react';
import { Provider } from 'react-redux';
import { combineReducers, createStore } from 'redux';

import MockNavigator from '__tests__/components/MockNavigator';
import { mockLaoId } from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';

import { mockDigitalCashContextValue, mockRollCall } from '../../../__tests__/utils';
import { digitalCashReducer } from '../../../reducer';
import SendReceive from '../SendReceive';

const mockStore = createStore(combineReducers({ ...digitalCashReducer }));

describe('SendReceive', () => {
  it('renders correctly', () => {
    const { toJSON } = render(
      <Provider store={mockStore}>
        <FeatureContext.Provider value={mockDigitalCashContextValue(true)}>
          <MockNavigator
            component={SendReceive}
            params={{ laoId: mockLaoId, rollCallId: mockRollCall.id.valueOf() }}
          />
        </FeatureContext.Provider>
      </Provider>,
    );
    expect(toJSON()).toMatchSnapshot();
  });

  it('renders correctly with passed scanned pop token', () => {
    const { toJSON } = render(
      <Provider store={mockStore}>
        <FeatureContext.Provider value={mockDigitalCashContextValue(true)}>
          <MockNavigator
            component={SendReceive}
            params={{
              laoId: mockLaoId,
              rollCallId: mockRollCall.id.valueOf(),
              scannedPoPToken: 'some pop token',
            }}
          />
        </FeatureContext.Provider>
      </Provider>,
    );
    expect(toJSON()).toMatchSnapshot();
  });
});
