import { useNavigation } from '@react-navigation/core';
import { createStackNavigator } from '@react-navigation/stack';
import React, { useEffect } from 'react';
import { useToast } from 'react-native-toast-notifications';

import { AppScreen } from 'core/navigation/AppNavigation';
import { FOUR_SECONDS } from 'resources/const';
import STRINGS from 'resources/strings';

import { HomeHooks } from '../hooks';
import { ConnectConfirm, ConnectOpenScan, Launch } from '../screens';

const Stack = createStackNavigator();

export default function ConnectNavigation() {
  // FIXME: route should use proper type
  const navigation = useNavigation<any>();
  const toast = useToast();

  const hasSeed = HomeHooks.useHasSeed();

  useEffect(() => {
    // do not allow a connection to a lao without first setting up the wallet
    if (!hasSeed) {
      toast.show(`Before connecting to a LAO, you need to set up the wallet!`, {
        type: 'danger',
        placement: 'top',
        duration: FOUR_SECONDS,
      });
      navigation.navigate(STRINGS.navigation_tab_wallet);
    }
  }, [navigation, toast, hasSeed]);

  return (
    <Stack.Navigator
      screenOptions={{
        headerShown: false,
      }}>
      <Stack.Screen name={STRINGS.connect_scanning_title} component={ConnectOpenScan} />
      <Stack.Screen name={STRINGS.connect_confirm_title} component={ConnectConfirm} />
      <Stack.Screen name={STRINGS.navigation_tab_launch} component={Launch} />
    </Stack.Navigator>
  );
}

export const ConnectNavigationScreen: AppScreen = {
  id: STRINGS.navigation_tab_connect,
  title: STRINGS.navigation_tab_connect,
  component: ConnectNavigation,
};
