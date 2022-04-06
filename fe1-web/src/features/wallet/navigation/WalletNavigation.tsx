import { createStackNavigator } from '@react-navigation/stack';
import React from 'react';

import STRINGS from 'resources/strings';

import { WalletError, WalletHome, WalletSetSeed, WalletSetup, WalletCreateSeed } from '../screens';

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
      <Stack.Screen name={STRINGS.navigation_wallet_setup_tab} component={WalletSetup} />
      <Stack.Screen name={STRINGS.navigation_wallet_home_tab} component={WalletHome} />
      <Stack.Screen name={STRINGS.navigation_wallet_create_seed} component={WalletCreateSeed} />
      <Stack.Screen name={STRINGS.navigation_wallet_insert_seed} component={WalletSetSeed} />
      <Stack.Screen name={STRINGS.navigation_wallet_error} component={WalletError} />
    </Stack.Navigator>
  );
}
