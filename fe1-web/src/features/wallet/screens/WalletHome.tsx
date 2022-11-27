import React, { useMemo } from 'react';
import { View } from 'react-native';

import ScreenWrapper from 'core/components/ScreenWrapper';
import { List } from 'core/styles';

import { rollCallWalletItemGenerator } from '../components/RollCallWalletItems';
import { WalletHooks } from '../hooks';

/**
 * Wallet UI once the wallet is synced
 */
const WalletHome = () => {
  const lao = WalletHooks.useCurrentLao();

  const generators = WalletHooks.useWalletItemGenerators();

  const walletItemGenerators = useMemo(() => {
    return [...generators, rollCallWalletItemGenerator].sort((a, b) => a.order - b.order);
  }, [generators]);

  return (
    <ScreenWrapper>
      <View style={List.container}>
        {walletItemGenerators.map((Generator) => (
          <Generator.ListItems key={Generator.order.toString()} laoId={lao.id} />
        ))}
      </View>
    </ScreenWrapper>
  );
};

export default WalletHome;
