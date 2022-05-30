import { useNavigation } from '@react-navigation/core';
import { createStackNavigator, StackScreenProps } from '@react-navigation/stack';
import React, { useState } from 'react';
import { TouchableOpacity } from 'react-native-gesture-handler';

import OptionsIcon from 'core/components/icons/OptionsIcon';
import WalletIcon from 'core/components/icons/WalletIcon';
import { useActionSheet } from 'core/hooks/ActionSheet';
import { AppParamList } from 'core/navigation/typing/AppParamList';
import { WalletParamList } from 'core/navigation/typing/WalletParamList';
import { Color, Icon, Spacing, Typography } from 'core/styles';
import STRINGS from 'resources/strings';

import { WalletFeature } from '../interface';
import { forget } from '../objects';
import { clearDummyWalletState, createDummyWalletState } from '../objects/DummyWallet';
import { WalletHome } from '../screens';
import WalletSingleRollCall, {
  ViewSingleRollCallScreenHeader,
  WalletSingleHeaderRight,
} from '../screens/WalletSingleRollCall';

const Stack = createStackNavigator<WalletParamList>();

/* can be in the lao or home navigation but we only need the top app navigation which is always present */
type NavigationProps = StackScreenProps<AppParamList, typeof STRINGS.navigation_app_lao>;

const WalletNavigationHeaderRight = () => {
  const navigation = useNavigation<NavigationProps['navigation']>();

  const showActionSheet = useActionSheet();

  const [isDebug, setIsDebug] = useState(false);

  const onPressOptions = () => {
    showActionSheet([
      {
        displayName: STRINGS.wallet_home_logout,
        action: () => {
          forget();
          navigation.navigate(STRINGS.navigation_app_wallet_create_seed);
        },
      },
      {
        displayName: STRINGS.wallet_home_toggle_debug,
        action: () => {
          if (isDebug) {
            clearDummyWalletState();
            setIsDebug(false);
          } else {
            createDummyWalletState().then(() => setIsDebug(true));
          }
        },
      },
    ]);
  };

  return (
    <TouchableOpacity onPress={onPressOptions}>
      <OptionsIcon color={Color.inactive} size={Icon.size} />
    </TouchableOpacity>
  );
};

/**
 * Defines the Wallet stack navigation.
 * Allows to navigate between the wallet screens.
 */
export default function WalletNavigation() {
  return (
    <Stack.Navigator
      screenOptions={{
        headerLeftContainerStyle: {
          paddingLeft: Spacing.contentSpacing,
        },
        headerRightContainerStyle: {
          paddingRight: Spacing.contentSpacing,
        },
        headerTitleStyle: Typography.topNavigationHeading,
        headerTitleAlign: 'center',
      }}>
      <Stack.Screen
        name={STRINGS.navigation_wallet_home}
        component={WalletHome}
        options={{
          headerTitle: STRINGS.navigation_wallet_home_title,
          headerRight: WalletNavigationHeaderRight,
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
    </Stack.Navigator>
  );
}

export const WalletNavigationScreen: WalletFeature.HomeScreen & WalletFeature.LaoScreen = {
  id: STRINGS.navigation_home_wallet,
  Component: WalletNavigation,
  tabBarIcon: WalletIcon,
  order: 99999999,
  headerShown: false,
};
