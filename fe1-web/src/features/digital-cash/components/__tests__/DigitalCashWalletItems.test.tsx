import { render } from '@testing-library/react-native';
import React from 'react';

import MockNavigator from '__tests__/components/MockNavigator';
import { mockLaoIdHash } from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';

import { mockDigitalCashContextValue } from '../../__tests__/utils';
import { DigitalCashHooks } from '../../hooks';
import DigitalCashWalletItems from '../DigitalCashWalletItems';

jest.mock('features/digital-cash/hooks');

jest.mock('@expo/vector-icons', () => {
  const MockIcon = ({ name, size, color }: { name: string; size: number; color: string }) =>
    `[name='${name}' size='${size}' color='${color}']`;

  return {
    Ionicons: MockIcon,
    MaterialIcons: MockIcon,
    MaterialCommunityIcons: MockIcon,
  };
});

(DigitalCashHooks.useTotalBalance as jest.Mock).mockReturnValue(10);

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
