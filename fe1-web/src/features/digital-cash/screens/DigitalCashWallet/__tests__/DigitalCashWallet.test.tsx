import { render } from '@testing-library/react-native';
import React from 'react';

import MockNavigator from '__tests__/components/MockNavigator';
import { mockLaoId, mockLaoIdHash } from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';
import {
  DigitalCashReactContext,
  DIGITAL_CASH_FEATURE_IDENTIFIER,
} from 'features/digital-cash/interface';

import DigitalCashWallet from '../DigitalCashWallet';

const contextValue = (isOrganizer: boolean) => ({
  [DIGITAL_CASH_FEATURE_IDENTIFIER]: {
    useCurrentLaoId: () => mockLaoIdHash,
    useIsLaoOrganizer: () => isOrganizer,
  } as DigitalCashReactContext,
});

describe('DigitalCashWallet', () => {
  it('renders correctly for organizers', () => {
    const { toJSON } = render(
      <FeatureContext.Provider value={contextValue(true)}>
        <MockNavigator component={DigitalCashWallet} params={{ laoId: mockLaoId }} />
      </FeatureContext.Provider>,
    );
    expect(toJSON()).toMatchSnapshot();
  });

  it('renders correctly for non-organizers', () => {
    const { toJSON } = render(
      <FeatureContext.Provider value={contextValue(false)}>
        <MockNavigator component={DigitalCashWallet} params={{ laoId: mockLaoId }} />
      </FeatureContext.Provider>,
    );
    expect(toJSON()).toMatchSnapshot();
  });
});
