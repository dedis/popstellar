import React from 'react';
import { createStackNavigator } from '@react-navigation/stack';

import STRINGS from 'resources/strings';

import {
  WalletError,
  WalletHome,
  WalletSetSeed,
  WalletShowSeed,
  WalletSyncedSeed,
} from '../screens';

const Stack = createStackNavigator();

/**
 * Defines the Wallet stack navigation.
 * Allows to navigate between the wallet screens.
 */
export default function WalletNavigation() {
  return (
    <Stack.Navigator
      screenOptions={{
        headerShown: false,
      }}>
      <Stack.Screen name={STRINGS.navigation_home_tab_wallet} component={WalletHome} />
      <Stack.Screen name={STRINGS.navigation_show_seed_wallet} component={WalletShowSeed} />
      <Stack.Screen name={STRINGS.navigation_insert_seed_tab_wallet} component={WalletSetSeed} />
      <Stack.Screen name={STRINGS.navigation_synced_wallet} component={WalletSyncedSeed} />
      <Stack.Screen name={STRINGS.navigation_wallet_error} component={WalletError} />
    </Stack.Navigator>
  );
}
