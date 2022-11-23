import React from 'react';
import { View } from 'react-native';

import ScreenWrapper from 'core/components/ScreenWrapper';
import { List } from 'core/styles';

import RollCallWalletItems from '../components/RollCallWalletItems';
import { WalletHooks } from '../hooks';

/**
 * Wallet UI once the wallet is synced
 */
const WalletHome = () => {
  const lao = WalletHooks.useCurrentLao();

  return (
    <ScreenWrapper>
      <View style={List.container}>
        <RollCallWalletItems laoId={lao.id} />
      </View>
    </ScreenWrapper>
  );
};

export default WalletHome;
