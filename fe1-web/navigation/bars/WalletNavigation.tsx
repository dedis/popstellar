import React from 'react';
import { createStackNavigator } from '@react-navigation/stack';

import STRINGS from 'res/strings';

import WalletHome from 'parts/wallet/WalletHome';
import WalletSetSeed from 'parts/wallet/WalletSetSeed';
import WalletShowSeed from 'parts/wallet/WalletShowSeed';
import WalletSyncedSeed from 'parts/wallet/WalletSyncedSeed';

/**
 * Define the Witness stack navigation
 * Allows to navigate to the Witness and the WitnessCamera screen
 */

const Stack = createStackNavigator();

export default function WalletNavigation() {
  return (
    <Stack.Navigator
      screenOptions={{
        headerShown: false,
      }}
    >
      <Stack.Screen
        name={STRINGS.navigation_home_tab_wallet}
        component={WalletHome}
      />
      <Stack.Screen
        name={STRINGS.navigation_show_seed_wallet}
        component={WalletShowSeed}
      />
      <Stack.Screen
        name={STRINGS.navigation_insert_seed_tab_wallet}
        component={WalletSetSeed}
      />
      <Stack.Screen
        name={STRINGS.navigation_synced_wallet}
        component={WalletSyncedSeed}
      />
    </Stack.Navigator>
  );
}
