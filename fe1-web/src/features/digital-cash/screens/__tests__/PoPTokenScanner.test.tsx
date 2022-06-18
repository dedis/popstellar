import { render } from '@testing-library/react-native';
import React from 'react';

import MockNavigator from '__tests__/components/MockNavigator';
import { mockLaoId } from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';

import { mockDigitalCashContextValue, mockRollCall } from '../../__tests__/utils';
import PoPTokenScanner from '../PoPTokenScanner';

describe('DigitalCashWallet', () => {
  it('renders correctly for organizers', () => {
    const { toJSON } = render(
      <FeatureContext.Provider value={mockDigitalCashContextValue(true)}>
        <MockNavigator
          component={PoPTokenScanner}
          params={{ laoId: mockLaoId, rollCallId: mockRollCall.id.valueOf(), beneficiaryIndex: 0 }}
        />
      </FeatureContext.Provider>,
    );
    expect(toJSON()).toMatchSnapshot();
  });

  it('renders correctly for non-organizers', () => {
    const { toJSON } = render(
      <FeatureContext.Provider value={mockDigitalCashContextValue(false)}>
        <MockNavigator
          component={PoPTokenScanner}
          params={{ laoId: mockLaoId, rollCallId: mockRollCall.id.valueOf(), beneficiaryIndex: 0 }}
        />
      </FeatureContext.Provider>,
    );
    expect(toJSON()).toMatchSnapshot();
  });
});
