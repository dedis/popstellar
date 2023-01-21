import { render } from '@testing-library/react-native';
import React from 'react';

import MockNavigator from '__tests__/components/MockNavigator';
import { mockPopToken } from '__tests__/utils';
import { mockRollCall } from 'features/rollCall/__tests__/utils';

import WalletSingleRollCall from '../WalletSingleRollCall';

describe('WalletSingleRollCall', () => {
  it('renders correctly', () => {
    const component = render(
      <MockNavigator
        component={WalletSingleRollCall}
        params={{
          rollCallId: mockRollCall.id.valueOf(),
          rollCallName: mockRollCall.name,
          rollCallTokenPublicKey: mockPopToken.publicKey.valueOf(),
        }}
      />,
    ).toJSON();
    expect(component).toMatchSnapshot();
  });
});

describe('ViewSingleRollCallScreenHeader', () => {
  it('renders correctly', () => {
    const component = render(
      <MockNavigator
        component={WalletSingleRollCall}
        params={{
          rollCallId: mockRollCall.id.valueOf(),
          rollCallName: mockRollCall.name,
          rollCallTokenPublicKey: mockPopToken.publicKey.valueOf(),
        }}
      />,
    ).toJSON();
    expect(component).toMatchSnapshot();
  });
});
