import { createStackNavigator } from '@react-navigation/stack';
import React from 'react';

import DrawerMenuButton from 'core/components/DrawerMenuButton';
import { makeIcon } from 'core/components/PoPIcon';
import { stackScreenOptionsWithHeader } from 'core/navigation/ScreenOptions';
import { LinkedOrganizationsParamList } from 'core/navigation/typing/LinkedOrganizationsParamList';
import STRINGS from 'resources/strings';

import { LinkedOrganizationsFeature } from '../interface';
import LinkedOrganizationsScreen from '../screens/LinkedOrganizationsScreen';

const LinkedOrganizationsNavigator = createStackNavigator<LinkedOrganizationsParamList>();

const LinkedOrganizationsNavigation = () => {
  return (
    <LinkedOrganizationsNavigator.Navigator
      initialRouteName={STRINGS.navigation_linked_organizations}
      screenOptions={stackScreenOptionsWithHeader}>
      <LinkedOrganizationsNavigator.Screen
        name={STRINGS.navigation_linked_organizations}
        component={LinkedOrganizationsScreen}
        options={{
          headerTitle: STRINGS.navigation_linked_organizations_title,
          headerLeft: DrawerMenuButton,
        }}
      />
    </LinkedOrganizationsNavigator.Navigator>
  );
};

export default LinkedOrganizationsNavigation;

// TODO: change Icon
export const LinkedOrganizationsLaoScreen: LinkedOrganizationsFeature.LaoScreen = {
  id: STRINGS.navigation_lao_linked_organizations,
  Component: LinkedOrganizationsNavigation,
  Icon: makeIcon('link'),
  headerShown: false,
  order: 100000000000000,
};
