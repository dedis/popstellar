import { createNavigationContainerRef } from '@react-navigation/core';
import { createStackNavigator } from '@react-navigation/stack';
import PropTypes from 'prop-types';
import React from 'react';
import { StyleSheet } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import STRINGS from 'resources/strings';

import { stackScreenOptionsWithoutHeader } from './ScreenOptions';
import { AppParamList } from './typing/AppParamList';
import { NavigationScreen } from './typing/Screen';

// allows react-navigation to be used outside of react components
export const navigationRef = createNavigationContainerRef<AppParamList>();

/**
 * Define the App stack navigation
 * Contains to navigation: the home navigation and the organization navigation
 */

const Stack = createStackNavigator<AppParamList>();

const styles = StyleSheet.create({
  view: {
    flex: 1,
  },
});

const AppNavigation = ({ screens }: IPropTypes) => {
  const entries = screens.map(({ id, title, Component }) => (
    // make the reasonable assumption that we haven't passed strings as components here
    <Stack.Screen name={id} key={id} component={Component} options={{ title: title || id }} />
  ));

  return (
    <SafeAreaView style={styles.view}>
      <Stack.Navigator
        initialRouteName={STRINGS.navigation_app_wallet_create_seed}
        screenOptions={stackScreenOptionsWithoutHeader}>
        {entries}
      </Stack.Navigator>
    </SafeAreaView>
  );
};

const propTypes = {
  screens: PropTypes.arrayOf(
    PropTypes.shape({
      id: PropTypes.string.isRequired,
      title: PropTypes.string,
      Component: PropTypes.elementType.isRequired,
    }).isRequired,
  ).isRequired,
};
AppNavigation.propTypes = propTypes;

AppNavigation.defaultProps = {};

export interface AppScreen extends NavigationScreen {
  id: keyof AppParamList;
}

type IPropTypes = Omit<PropTypes.InferProps<typeof propTypes>, 'screens'> & {
  screens: AppScreen[];
};

export default AppNavigation;
