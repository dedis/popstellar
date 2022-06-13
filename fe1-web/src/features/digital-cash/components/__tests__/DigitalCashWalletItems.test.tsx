import { render } from '@testing-library/react-native';
import React from 'react';

import MockNavigator from '__tests__/components/MockNavigator';
import { mockLaoIdHash } from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';

import { mockDigitalCashContextValue } from '../../__tests__/utils';
import DigitalCashWalletItems from '../DigitalCashWalletItems';

describe('DigitalCashWalletItems', () => {
  it('renders correctly for organizers', () => {
    const Screen = () => <DigitalCashWalletItems laoId={mockLaoIdHash} />;

    const { toJSON } = render(
      <FeatureContext.Provider value={mockDigitalCashContextValue(true)}>
        <MockNavigator component={Screen} />
      </FeatureContext.Provider>,
    );
    expect(toJSON()).toMatchSnapshot();
  });
});
