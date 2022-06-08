import { render } from '@testing-library/react-native';
import React from 'react';

import MockNavigator from '__tests__/components/MockNavigator';
import { mockLaoId, mockLaoIdHash } from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';
import {
  DigitalCashReactContext,
  DIGITAL_CASH_FEATURE_IDENTIFIER,
} from 'features/digital-cash/interface';
import { mockRollCall } from 'features/rollCall/__tests__/utils';

import SendReceive from '../SendReceive';

const contextValue = (isOrganizer: boolean) => ({
  [DIGITAL_CASH_FEATURE_IDENTIFIER]: {
    useCurrentLaoId: () => mockLaoIdHash,
    useIsLaoOrganizer: () => isOrganizer,
  } as DigitalCashReactContext,
});

describe('SendReceive', () => {
  it('renders correctly', () => {
    const { toJSON } = render(
      <FeatureContext.Provider value={contextValue(true)}>
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
      <FeatureContext.Provider value={contextValue(true)}>
        <MockNavigator
          component={SendReceive}
          params={{
            laoId: mockLaoId,
            rollCallId: mockRollCall.id.valueOf(),
            scannedPoPTokenBeneficiaryIndex: 0,
            scannedPoPToken: 'some pop token',
          }}
        />
      </FeatureContext.Provider>,
    );
    expect(toJSON()).toMatchSnapshot();
  });
});
