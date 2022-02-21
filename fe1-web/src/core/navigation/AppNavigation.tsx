import React from 'react';
import { StyleSheet } from 'react-native';
import { createStackNavigator } from '@react-navigation/stack';
import { SafeAreaView } from 'react-native-safe-area-context';
import PropTypes from 'prop-types';

/**
 * Define the App stack navigation
 * Contains to navigation: the home navigation and the organization navigation
 */

const Stack = createStackNavigator();

const styles = StyleSheet.create({
  view: {
    flex: 1,
  },
});

const AppNavigation = ({ screens }: IPropTypes) => {
  const entries = screens.map(({ name, component }) => (
    // make the reasonable assumption that we haven't passed strings as components here
    <Stack.Screen name={name} key={name} component={component as React.ComponentType} />
  ));

  return (
    <SafeAreaView style={styles.view}>
      <Stack.Navigator
        screenOptions={{
          headerShown: false,
        }}>
        {entries}
      </Stack.Navigator>
    </SafeAreaView>
  );
};

const propTypes = {
  screens: PropTypes.arrayOf(
    PropTypes.shape({
      name: PropTypes.string.isRequired,
      component: PropTypes.elementType.isRequired,
    }).isRequired,
  ).isRequired,
};
AppNavigation.propTypes = propTypes;

AppNavigation.defaultProps = {};

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default AppNavigation;
