import { useActionSheet } from '@expo/react-native-action-sheet';
import { useNavigation } from '@react-navigation/core';
import { createStackNavigator, StackScreenProps } from '@react-navigation/stack';
import React from 'react';
import { TouchableOpacity } from 'react-native-gesture-handler';

import { Icon } from 'core/components';
import { makeIcon } from 'core/components/Icon';
import { AppParamList } from 'core/navigation/typing/AppParamList';
import { WalletParamList } from 'core/navigation/typing/WalletParamList';
import { Colors } from 'core/styles';
import STRINGS from 'resources/strings';

import { WalletFeature } from '../interface';
import { forget } from '../objects';
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
      <Icon name="options" color={Colors.primary} size={25} />
    </TouchableOpacity>
  );
};

export const WalletNavigationScreen: WalletFeature.HomeScreen & WalletFeature.LaoScreen = {
  id: STRINGS.navigation_home_wallet,
  Component: WalletNavigation,
  tabBarIcon: makeIcon('wallet'),
  order: 99999999,
  headerRight: () => <WalletNavigationHeaderRight />,
};
