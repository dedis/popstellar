import { createStackNavigator } from '@react-navigation/stack';
import React from 'react';

import STRINGS from 'resources/strings';

import { WalletError, WalletHome, WalletSetSeed, WalletSetup, WalletCreateSeed } from '../screens';
import { WalletStore } from '../store';

const Stack = createStackNavigator();

/**
 * Defines the Wallet stack navigation.
 * Allows to navigate between the wallet screens.
 */
export default function WalletNavigation() {
  const home = () => {
    return <Stack.Screen name={STRINGS.navigation_wallet_home_tab} component={WalletHome} />;
  };
  const setup = () => {
    return <Stack.Screen name={STRINGS.navigation_wallet_setup_tab} component={WalletSetup} />;
  };
  const hasSeed = WalletStore.hasSeed();
  return (
    <Stack.Navigator
      screenOptions={{
        headerShown: false,
      }}>
      {hasSeed ? home() : setup()}
      {hasSeed ? setup() : home()}
      <Stack.Screen name={STRINGS.navigation_wallet_create_seed} component={WalletCreateSeed} />
      <Stack.Screen name={STRINGS.navigation_wallet_insert_seed} component={WalletSetSeed} />
      <Stack.Screen name={STRINGS.navigation_wallet_error} component={WalletError} />
    </Stack.Navigator>
  );
}
