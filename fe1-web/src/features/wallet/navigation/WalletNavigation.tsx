import { createStackNavigator } from '@react-navigation/stack';
import React from 'react';

import ButtonPadding from 'core/components/ButtonPadding';
import DrawerMenuButton from 'core/components/DrawerMenuButton';
import { makeIcon } from 'core/components/PoPIcon';
import { stackScreenOptionsWithHeader } from 'core/navigation/ScreenOptions';
import { WalletParamList } from 'core/navigation/typing/WalletParamList';
import STRINGS from 'resources/strings';

import { WalletFeature } from '../interface';
import { WalletHome } from '../screens';
import WalletSingleRollCall, {
  ViewSingleRollCallScreenHeader,
} from '../screens/WalletSingleRollCall';

const Stack = createStackNavigator<WalletParamList>();

/**
 * Defines the Wallet stack navigation.
 * Allows to navigate between the wallet screens.
 */
export default function WalletNavigation() {
  return (
    <Stack.Navigator screenOptions={stackScreenOptionsWithHeader}>
      <Stack.Screen
        name={STRINGS.navigation_wallet_home}
        component={WalletHome}
        options={{
          headerTitle: STRINGS.navigation_wallet_home_title,
          headerLeft: DrawerMenuButton,
          headerRight: ButtonPadding,
        }}
      />
      <Stack.Screen
        name={STRINGS.navigation_wallet_single_roll_call}
        component={WalletSingleRollCall}
        options={{
          headerTitle: ViewSingleRollCallScreenHeader,
        }}
      />
    </Stack.Navigator>
  );
}

export const WalletNavigationScreen: WalletFeature.LaoScreen = {
  id: STRINGS.navigation_lao_wallet,
  title: STRINGS.navigation_lao_wallet_title,
  Component: WalletNavigation,
  Icon: makeIcon('qrCode'),
  order: 99999999,
  headerShown: false,
};
