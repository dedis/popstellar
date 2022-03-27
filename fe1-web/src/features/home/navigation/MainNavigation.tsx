import { createMaterialTopTabNavigator } from '@react-navigation/material-top-tabs';
import React from 'react';
import { Platform, StyleSheet } from 'react-native';

import { ConnectNavigation } from 'features/connect/navigation';
import { WalletNavigation } from 'features/wallet/navigation';
import STRINGS from 'resources/strings';

import { Home, Launch } from '../screens';

/**
 * The main tab navigation component. It creates a tab navigator between the Home, Connect, Launch
 * and Wallet components.
 */
const HomeTopTabNavigator = createMaterialTopTabNavigator();

const styles = StyleSheet.create({
  navigator: {
    ...Platform.select({
      web: {
        width: '100vw',
      },
      default: {},
    }),
  },
});

const MainNavigation: React.FC = () => {
  return (
    <HomeTopTabNavigator.Navigator
      style={styles.navigator}
      initialRouteName={STRINGS.navigation_tab_home}
      screenOptions={{
        swipeEnabled: false,
      }}>
      <HomeTopTabNavigator.Screen name={STRINGS.navigation_tab_home} component={Home} />
      <HomeTopTabNavigator.Screen
        name={STRINGS.navigation_tab_connect}
        component={ConnectNavigation}
      />
      <HomeTopTabNavigator.Screen name={STRINGS.navigation_tab_launch} component={Launch} />
      <HomeTopTabNavigator.Screen
        name={STRINGS.navigation_tab_wallet}
        component={WalletNavigation}
      />
    </HomeTopTabNavigator.Navigator>
  );
};

export default MainNavigation;
