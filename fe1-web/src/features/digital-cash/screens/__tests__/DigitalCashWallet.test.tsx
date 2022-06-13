import { render } from '@testing-library/react-native';
import React from 'react';

import MockNavigator from '__tests__/components/MockNavigator';
import { mockLaoId } from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';

import { mockContextValue } from '../../__tests__/utils';
import DigitalCashWallet from '../DigitalCashWallet';

describe('DigitalCashWallet', () => {
  it('renders correctly for organizers', () => {
    const { toJSON } = render(
      <FeatureContext.Provider value={mockContextValue(true)}>
        <MockNavigator component={DigitalCashWallet} params={{ laoId: mockLaoId }} />
      </FeatureContext.Provider>,
    );
    expect(toJSON()).toMatchSnapshot();
  });

  it('renders correctly for non-organizers', () => {
    const { toJSON } = render(
      <FeatureContext.Provider value={mockContextValue(false)}>
        <MockNavigator component={DigitalCashWallet} params={{ laoId: mockLaoId }} />
      </FeatureContext.Provider>,
    );
    expect(toJSON()).toMatchSnapshot();
  });
});
