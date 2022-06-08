import { render } from '@testing-library/react-native';
import React from 'react';

import MockNavigator from '__tests__/components/MockNavigator';
import { mockLaoIdHash } from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';
import {
  DigitalCashReactContext,
  DIGITAL_CASH_FEATURE_IDENTIFIER,
} from 'features/digital-cash/interface';

import DigitalCashWalletItems from '../DigitalCashWalletItems';

const contextValue = {
  [DIGITAL_CASH_FEATURE_IDENTIFIER]: {
    useCurrentLaoId: () => mockLaoIdHash,
    useIsLaoOrganizer: () => false,
  } as DigitalCashReactContext,
};

describe('DigitalCashWalletItems', () => {
  it('renders correctly for organizers', () => {
    const Screen = () => <DigitalCashWalletItems laoId={mockLaoIdHash} />;

    const { toJSON } = render(
      <FeatureContext.Provider value={contextValue}>
        <MockNavigator component={Screen} />
      </FeatureContext.Provider>,
    );
    expect(toJSON()).toMatchSnapshot();
  });
});
