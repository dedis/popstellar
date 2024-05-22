import { createStackNavigator } from '@react-navigation/stack';
import React from 'react';

import DrawerMenuButton from 'core/components/DrawerMenuButton';
import { makeIcon } from 'core/components/PoPIcon';
import { stackScreenOptionsWithHeader } from 'core/navigation/ScreenOptions';
import { LinkedOrganizationsParamList } from 'core/navigation/typing/LinkedOrganizationsParamList';
import STRINGS from 'resources/strings';

import { LinkedOrganizationsFeature } from '../interface';
import LinkedOrganizationsScreen from '../screens/LinkedOrganizationsScreen';
import AddLinkedOrganizationButton from '../components/AddLinkedOrganizationButton';
import AddLinkedOrganizationModal from '../components/AddLinkedOrganizationModal';

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
          headerRight: AddLinkedOrganizationButton,
        }}
      />
      <LinkedOrganizationsNavigator.Screen
        name={STRINGS.linked_organizations_navigation_addlinkedorgModal}
        component={AddLinkedOrganizationModal}
        options={{
          presentation: 'transparentModal',
          headerTitle: STRINGS.navigation_linked_organizations_title,

        }}
      />
    </LinkedOrganizationsNavigator.Navigator>
  );
};

export default LinkedOrganizationsNavigation;

export const LinkedOrganizationsLaoScreen: LinkedOrganizationsFeature.LaoScreen = {
  id: STRINGS.navigation_lao_linked_organizations,
  Component: LinkedOrganizationsNavigation,
  Icon: makeIcon('link'),
  headerShown: false,
  order: 100000000000000,
};
