import { render } from '@testing-library/react-native';
import React from 'react';

import MockNavigator from '__tests__/components/MockNavigator';
import { mockLao, mockLaoId, mockLaoIdHash, mockLaoName } from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';
import { mockRollCall } from 'features/rollCall/__tests__/utils';
import {
  WalletFeature,
  WalletReactContext,
  WALLET_FEATURE_IDENTIFIER,
} from 'features/wallet/interface';

import RollCallWalletItems from '../RollCallWalletItems';

const contextValue = (useRollCallsByLaoId: Record<string, WalletFeature.RollCall>) => ({
  [WALLET_FEATURE_IDENTIFIER]: {
    useCurrentLaoId: () => mockLaoIdHash,
    useCurrentLao: () => mockLao,
    useConnectedToLao: () => true,
    useLaoIds: () => [],
    useRollCallTokensByLaoId: () => [],
    useNamesByLaoId: () => ({ [mockLaoId]: mockLaoName }),
    useRollCallsByLaoId: () => useRollCallsByLaoId,
    walletItemGenerators: [],
    walletNavigationScreens: [],
  } as WalletReactContext,
});

describe('RollCallWalletItems', () => {
  it('renders correctly with roll calls', () => {
    const Screen = () => <RollCallWalletItems laoId={mockLaoIdHash} />;

    const { toJSON } = render(
      <FeatureContext.Provider value={contextValue({ [mockRollCall.id.valueOf()]: mockRollCall })}>
        <MockNavigator component={Screen} />
      </FeatureContext.Provider>,
    );
    expect(toJSON()).toMatchSnapshot();
  });

  it('renders correctly without roll calls', () => {
    const Screen = () => <RollCallWalletItems laoId={mockLaoIdHash} />;

    const { toJSON } = render(
      <FeatureContext.Provider value={contextValue({})}>
        <MockNavigator component={Screen} />
      </FeatureContext.Provider>,
    );
    expect(toJSON()).toMatchSnapshot();
  });
});
