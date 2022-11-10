import { createStackNavigator } from '@react-navigation/stack';
import React from 'react';

import { makeIcon } from 'core/components/PoPIcon';
import { stackScreenOptionsWithHeader } from 'core/navigation/ScreenOptions';
import { WalletParamList } from 'core/navigation/typing/WalletParamList';
import STRINGS from 'resources/strings';

import { WalletHooks } from '../hooks';
import { WalletFeature } from '../interface';
import { WalletHome } from '../screens';
import WalletSingleRollCall, {
  ViewSingleRollCallScreenHeader,
  WalletSingleHeaderRight,
} from '../screens/WalletSingleRollCall';

const Stack = createStackNavigator<WalletParamList>();

/**
 * Defines the Wallet stack navigation.
 * Allows to navigate between the wallet screens.
 */
export default function WalletNavigation() {
  const screens = WalletHooks.useWalletNavigationScreens();

  return (
    <Stack.Navigator screenOptions={stackScreenOptionsWithHeader}>
      <Stack.Screen
        name={STRINGS.navigation_wallet_home}
        component={WalletHome}
        options={{
          headerTitle: STRINGS.navigation_wallet_home_title,
          headerLeft: () => null,
        }}
      />
      <Stack.Screen
        name={STRINGS.navigation_wallet_single_roll_call}
        component={WalletSingleRollCall}
        options={{
          headerTitle: ViewSingleRollCallScreenHeader,
          headerRight: WalletSingleHeaderRight,
        }}
      />
      {screens.map(
        ({ id, title, headerTitle, headerLeft, headerRight, headerShown, Component }) => (
          <Stack.Screen
            name={id}
            key={id}
            component={Component}
            options={{
              title: title || id,
              headerTitle: headerTitle || title || id,
              headerLeft: headerLeft || stackScreenOptionsWithHeader.headerLeft,
              headerRight: headerRight || stackScreenOptionsWithHeader.headerRight,
              headerShown,
            }}
          />
        ),
      )}
    </Stack.Navigator>
  );
}

export const WalletNavigationScreen: WalletFeature.LaoScreen = {
  id: STRINGS.navigation_home_wallet,
  Component: WalletNavigation,
  tabBarIcon: makeIcon('wallet'),
  order: 99999999,
  headerShown: false,
};
