import { useActionSheet } from '@expo/react-native-action-sheet';
import { useNavigation } from '@react-navigation/core';
import { createStackNavigator, StackScreenProps } from '@react-navigation/stack';
import React, { useState } from 'react';
import { TouchableOpacity } from 'react-native-gesture-handler';

import OptionsIcon from 'core/components/icons/OptionsIcon';
import WalletIcon from 'core/components/icons/WalletIcon';
import { AppParamList } from 'core/navigation/typing/AppParamList';
import { WalletParamList } from 'core/navigation/typing/WalletParamList';
import { Color, Icon } from 'core/styles';
import STRINGS from 'resources/strings';

import { WalletFeature } from '../interface';
import { forget } from '../objects';
import { clearDummyWalletState, createDummyWalletState } from '../objects/DummyWallet';
import { WalletHome } from '../screens';

const Stack = createStackNavigator<WalletParamList>();

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

/* can be in the lao or home navigation but we only need the top app navigation which is always present */
type NavigationProps = StackScreenProps<AppParamList, typeof STRINGS.navigation_app_lao>;

const WalletNavigationHeaderRight = () => {
  const navigation = useNavigation<NavigationProps['navigation']>();

  const { showActionSheetWithOptions } = useActionSheet();

  const [isDebug, setIsDebug] = useState(false);

  const onPressOptions = () => {
    showActionSheetWithOptions(
      {
        options: [
          STRINGS.wallet_home_logout,
          STRINGS.wallet_home_toggle_debug,
          STRINGS.general_button_cancel,
        ],
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
      <OptionsIcon color={Color.inactive} size={Icon.size} />
    </TouchableOpacity>
  );
};

export const WalletNavigationScreen: WalletFeature.HomeScreen & WalletFeature.LaoScreen = {
  id: STRINGS.navigation_home_wallet,
  Component: WalletNavigation,
  tabBarIcon: WalletIcon,
  order: 99999999,
  headerRight: () => <WalletNavigationHeaderRight />,
};
