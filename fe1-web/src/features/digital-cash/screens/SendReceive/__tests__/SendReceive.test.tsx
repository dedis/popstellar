import { render } from '@testing-library/react-native';
import React from 'react';

import MockNavigator from '__tests__/components/MockNavigator';
import { mockLaoId } from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';

import { mockContextValue, mockRollCall } from '../../../__tests__/utils';
import SendReceive from '../SendReceive';

describe('SendReceive', () => {
  it('renders correctly', () => {
    const { toJSON } = render(
      <FeatureContext.Provider value={mockContextValue(true)}>
        <MockNavigator
          component={SendReceive}
          params={{ laoId: mockLaoId, rollCallId: mockRollCall.id.valueOf() }}
        />
      </FeatureContext.Provider>,
    );
    expect(toJSON()).toMatchSnapshot();
  });

  it('renders correctly with passed scanned pop token', () => {
    const { toJSON } = render(
      <FeatureContext.Provider value={mockContextValue(true)}>
        <MockNavigator
          component={SendReceive}
          params={{
            laoId: mockLaoId,
            rollCallId: mockRollCall.id.valueOf(),
            scannedPoPToken: 'some pop token',
          }}
        />
      </FeatureContext.Provider>,
    );
    expect(toJSON()).toMatchSnapshot();
  });
});
