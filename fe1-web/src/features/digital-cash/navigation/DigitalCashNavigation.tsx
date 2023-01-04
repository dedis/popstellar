import { createStackNavigator } from '@react-navigation/stack';
import React from 'react';

import DrawerMenuButton from 'core/components/DrawerMenuButton';
import { makeIcon } from 'core/components/PoPIcon';
import { stackScreenOptionsWithHeader } from 'core/navigation/ScreenOptions';
import { DigitalCashParamList } from 'core/navigation/typing/DigitalCashParamList';
import STRINGS from 'resources/strings';

import { DigitalCashFeature } from '../interface';
import DigitalCashWallet from '../screens/DigitalCashWallet';
import PoPTokenScanner from '../screens/PoPTokenScanner';
import SendReceive from '../screens/SendReceive';
import { SendReceiveHeaderRight } from '../screens/SendReceive/SendReceive';

const DigitalCashNavigator = createStackNavigator<DigitalCashParamList>();

const DigitalCashNavigation = () => {
  return (
    <DigitalCashNavigator.Navigator
      initialRouteName={STRINGS.navigation_digital_cash_wallet}
      screenOptions={stackScreenOptionsWithHeader}>
      <DigitalCashNavigator.Screen
        name={STRINGS.navigation_digital_cash_wallet}
        component={DigitalCashWallet}
        options={{
          headerTitle: STRINGS.navigation_digital_cash_wallet_title,
          headerLeft: DrawerMenuButton,
        }}
      />
      <DigitalCashNavigator.Screen
        name={STRINGS.navigation_digital_cash_send_receive}
        component={SendReceive}
        options={{
          headerTitle: STRINGS.navigation_digital_cash_send_receive_title,
          headerRight: SendReceiveHeaderRight,
        }}
      />
      <DigitalCashNavigator.Screen
        name={STRINGS.navigation_digital_cash_wallet_scanner}
        component={PoPTokenScanner}
        options={{
          headerTitle: STRINGS.digital_cash_wallet_screen_title,
          headerShown: false,
        }}
      />
    </DigitalCashNavigator.Navigator>
  );
};

export default DigitalCashNavigation;

export const DigitalCashLaoScreen: DigitalCashFeature.LaoScreen = {
  id: STRINGS.navigation_lao_digital_cash,
  title: STRINGS.navigation_lao_digital_cash_title,
  Component: DigitalCashNavigation,
  Icon: makeIcon('digitalCash'),
  headerShown: false,
  order: 10000000,
};
