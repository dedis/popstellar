import { createStackNavigator } from '@react-navigation/stack';
import React from 'react';

import STRINGS from 'resources/strings';

import { WalletError, WalletHome } from '../screens';

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
      <Stack.Screen name={STRINGS.navigation_wallet_home_tab} component={WalletHome} />
      <Stack.Screen name={STRINGS.navigation_wallet_error} component={WalletError} />
    </Stack.Navigator>
  );
}
