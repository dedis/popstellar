import { useNavigation } from '@react-navigation/core';
import { createStackNavigator, StackScreenProps } from '@react-navigation/stack';
import React from 'react';
import { TouchableOpacity } from 'react-native-gesture-handler';

import { PoPIcon } from 'core/components';
import { makeIcon } from 'core/components/PoPIcon';
import { useActionSheet } from 'core/hooks/ActionSheet';
import { AppParamList } from 'core/navigation/typing/AppParamList';
import { WalletParamList } from 'core/navigation/typing/WalletParamList';
import { Color, Icon, Spacing, Typography } from 'core/styles';
import STRINGS from 'resources/strings';

import { WalletHooks } from '../hooks';
import { WalletFeature } from '../interface';
import { forget } from '../objects';
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

  const onPressOptions = () => {
    showActionSheet([
      {
        displayName: STRINGS.wallet_home_logout,
        action: () => {
          forget();
          navigation.navigate(STRINGS.navigation_app_wallet_create_seed);
        },
      },
    ]);
  };

  return (
    <TouchableOpacity onPress={onPressOptions}>
      <PoPIcon name="options" color={Color.inactive} size={Icon.size} />
    </TouchableOpacity>
  );
};

/**
 * Defines the Wallet stack navigation.
 * Allows to navigate between the wallet screens.
 */
export default function WalletNavigation() {
  const screens = WalletHooks.useWalletNavigationScreens();

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
      {screens.map(
        ({ id, title, headerTitle, headerLeft, headerRight, headerShown, Component }) => (
          <Stack.Screen
            name={id}
            key={id}
            component={Component}
            options={{
              title: title || id,
              headerTitle: headerTitle || title || id,
              headerLeft,
              headerRight,
              headerShown,
            }}
          />
        ),
      )}
    </Stack.Navigator>
  );
}

export const WalletNavigationScreen: WalletFeature.HomeScreen & WalletFeature.LaoScreen = {
  id: STRINGS.navigation_home_wallet,
  Component: WalletNavigation,
  tabBarIcon: makeIcon('wallet'),
  order: 99999999,
  headerShown: false,
};
