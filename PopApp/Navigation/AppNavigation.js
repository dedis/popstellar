import React from 'react';
import { StyleSheet } from 'react-native';
import { createStackNavigator } from '@react-navigation/stack';
import { SafeAreaView } from 'react-native-safe-area-context';
import { connect } from 'react-redux';

import STRINGS from '../res/strings';

import Navigation from './Navigation';
import OrganizationNavigation from './OrganizationNavigation';
import PROPS_TYPE from '../res/Props';

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

function AppNavigation({ lao }) {
  return (
    <SafeAreaView style={styles.view}>
      <Stack.Navigator
        screenOptions={{
          headerShown: false,
        }}
        initialRouteName={
          lao
            ? STRINGS.app_navigation_tab_organizer : STRINGS.app_navigation_tab_home
        }
      >
        <Stack.Screen
          name={STRINGS.app_navigation_tab_home}
          component={Navigation}
        />
        <Stack.Screen
          name={STRINGS.app_navigation_tab_organizer}
          component={OrganizationNavigation}
        />
      </Stack.Navigator>
    </SafeAreaView>
  );
}

AppNavigation.propTypes = {
  lao: PROPS_TYPE.LAO.isRequired,
};

const mapStateToProps = (state) => ({
  lao: state.currentLaoReducer.lao,
});

export default connect(mapStateToProps)(AppNavigation);
