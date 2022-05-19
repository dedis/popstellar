import { useActionSheet } from '@expo/react-native-action-sheet';
import { useNavigation } from '@react-navigation/core';
import { createStackNavigator } from '@react-navigation/stack';
import React, { useState } from 'react';
import { TouchableOpacity } from 'react-native-gesture-handler';

import OptionsIcon from 'core/components/icons/OptionsIcon';
import WalletIcon from 'core/components/icons/WalletIcon';
import { Colors } from 'core/styles';
import { HomeFeature } from 'features/home/interface';
import STRINGS from 'resources/strings';

import { forget } from '../objects';
import { clearDummyWalletState, createDummyWalletState } from '../objects/DummyWallet';
import { WalletHome } from '../screens';

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
    </Stack.Navigator>
  );
}

const WalletNavigationHeaderRight = () => {
  // FIXME: Navigation should use a defined type here (instead of any)
  const navigation = useNavigation<any>();

  const { showActionSheetWithOptions } = useActionSheet();

  const [isDebug, setIsDebug] = useState(false);

  const onPressOptions = () => {
    showActionSheetWithOptions(
      {
        options: ['Logout', 'Toggle debug mode', 'Cancel'],
        cancelButtonIndex: 2,
      },
      (idx) => {
        switch (idx) {
          case 0:
            // logout
            forget();
            navigation.navigate(STRINGS.navigation_app_wallet_create_seed);
            break;
          case 1:
            // toggle debug mode
            if (isDebug) {
              clearDummyWalletState();
              setIsDebug(false);
            } else {
              createDummyWalletState().then(() => setIsDebug(true));
            }
            break;
          case 2:
          default:
            // cancel
            break;
        }
      },
    );
  };

  return (
    <TouchableOpacity onPress={onPressOptions}>
      <OptionsIcon color={Colors.primary} size={25} />
    </TouchableOpacity>
  );
};

export const WalletNavigationScreen: HomeFeature.Screen = {
  id: STRINGS.navigation_home_tab_wallet,
  Component: WalletNavigation,
  tabBarIcon: WalletIcon,
  order: 99999999,
  headerRight: () => <WalletNavigationHeaderRight />,
};
