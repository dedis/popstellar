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
  const home = { name: STRINGS.navigation_wallet_home_tab, Component: WalletHome };
  const setup = { name: STRINGS.navigation_wallet_setup_tab, Component: WalletSetup };
  const hasSeed = WalletStore.hasSeed();

  const screens = hasSeed ? [home, setup] : [setup, home];
  return (
    <Stack.Navigator
      screenOptions={{
        headerShown: false,
      }}>
      {screens.map(({ name, Component }) => (
        <Stack.Screen name={name} component={Component} />
      ))}
      <Stack.Screen name={STRINGS.navigation_wallet_create_seed} component={WalletCreateSeed} />
      <Stack.Screen name={STRINGS.navigation_wallet_insert_seed} component={WalletSetSeed} />
      <Stack.Screen name={STRINGS.navigation_wallet_error} component={WalletError} />
    </Stack.Navigator>
  );
}
