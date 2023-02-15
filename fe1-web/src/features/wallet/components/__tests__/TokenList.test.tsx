import { render } from '@testing-library/react-native';
import React from 'react';

import MockNavigator from '__tests__/components/MockNavigator';
import { mockLao, serializedMockLaoId, mockLaoId, mockLaoName } from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';
import { mockRollCall } from 'features/rollCall/__tests__/utils';
import {
  WalletFeature,
  WalletReactContext,
  WALLET_FEATURE_IDENTIFIER,
} from 'features/wallet/interface';

import TokenList from '../TokenList';

const contextValue = (useRollCallsByLaoId: Record<string, WalletFeature.RollCall>) => ({
  [WALLET_FEATURE_IDENTIFIER]: {
    useCurrentLaoId: () => mockLaoId,
    useCurrentLao: () => mockLao,
    useConnectedToLao: () => true,
    useLaoIds: () => [],
    useRollCallTokensByLaoId: () => [],
    useNamesByLaoId: () => ({ [serializedMockLaoId]: mockLaoName }),
    useRollCallsByLaoId: () => useRollCallsByLaoId,
  } as WalletReactContext,
});

describe('RollCallWalletItems', () => {
  it('renders correctly with roll calls', () => {
    const Screen = () => <TokenList laoId={mockLaoId} />;

    const { toJSON } = render(
      <FeatureContext.Provider value={contextValue({ [mockRollCall.id.valueOf()]: mockRollCall })}>
        <MockNavigator component={Screen} />
      </FeatureContext.Provider>,
    );
    expect(toJSON()).toMatchSnapshot();
  });

  it('renders correctly without roll calls', () => {
    const Screen = () => <TokenList laoId={mockLaoId} />;

    const { toJSON } = render(
      <FeatureContext.Provider value={contextValue({})}>
        <MockNavigator component={Screen} />
      </FeatureContext.Provider>,
    );
    expect(toJSON()).toMatchSnapshot();
  });
});
