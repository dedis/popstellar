import { getFocusedRouteNameFromRoute } from '@react-navigation/core';
import {
  createDrawerNavigator,
  DrawerContentComponentProps,
  DrawerContentScrollView,
  DrawerItem,
  DrawerItemList,
} from '@react-navigation/drawer';
import Constants from 'expo-constants';
import React, { useMemo } from 'react';
import { StyleSheet, Text, View, ViewStyle } from 'react-native';

import NavigationPadding from 'core/components/NavigationPadding';
import { makeIcon } from 'core/components/PoPIcon';
import { AppScreen } from 'core/navigation/AppNavigation';
import { drawerNavigationOptions } from 'core/navigation/ScreenOptions';
import { LaoParamList } from 'core/navigation/typing/LaoParamList';
import { getNetworkManager } from 'core/network';
import { Color, Spacing, Typography } from 'core/styles';
import STRINGS from 'resources/strings';

import NoCurrentLaoErrorBoundary from '../errors/NoCurrentLaoErrorBoundary';
import { LaoHooks } from '../hooks';
import { LaoFeature } from '../interface';
import InviteScreen from '../screens/InviteScreen';
import EventsNavigation from './EventsNavigation';

const LaoNavigator = createDrawerNavigator<LaoParamList>();

const styles = StyleSheet.create({
  offlineHeader: {
    width: '100%',
    backgroundColor: Color.warning,
    paddingVertical: Spacing.x05,
    paddingHorizontal: Spacing.contentSpacing,
  } as ViewStyle,
  drawerWapper: {
    flex: 1,
    flexDirection: 'column',
    margin: Spacing.contentSpacing,
  } as ViewStyle,
  spacer: {} as ViewStyle,
  drawerFooter: {
    paddingBottom: Spacing.contentSpacing,
  } as ViewStyle,
  drawerHeader: {
    marginBottom: Spacing.x05,
  } as ViewStyle,
  drawerContentWrapper: {
    margin: 0,
  } as ViewStyle,
});

const OfflineHeader = () => {
  const isConnected = LaoHooks.useConnectedToLao();

  if (isConnected) {
    return null;
  }

  return (
    <View style={styles.offlineHeader}>
      <Text style={[Typography.base, Typography.small, Typography.centered, Typography.negative]}>
        {STRINGS.general_offline}
      </Text>
    </View>
  );
};

const wrapWithOfflineHeader = (Component: React.ComponentType<unknown>) => () => {
  return (
    <>
      <Component />
      <OfflineHeader />
    </>
  );
};

const DisconnectIcon = makeIcon('logout');

const LaoDrawerContent = ({ descriptors, navigation, state }: DrawerContentComponentProps) => {
  const lao = LaoHooks.useCurrentLao();

  return (
    <View style={styles.drawerWapper}>
      <DrawerContentScrollView style={styles.drawerContentWrapper}>
        <View style={styles.drawerHeader}>
          <Text style={[Typography.base, Typography.important]}>{lao.name}</Text>
        </View>
        <DrawerItemList navigation={navigation} descriptors={descriptors} state={state} />
        <DrawerItem
          label={STRINGS.navigation_lao_disconnect_title}
          onPress={() => {
            getNetworkManager().disconnectFromAll();

            navigation.navigate(STRINGS.navigation_app_home, {
              screen: STRINGS.navigation_home_home,
            });
          }}
          icon={DisconnectIcon}
          style={drawerNavigationOptions.drawerItemStyle}
          labelStyle={drawerNavigationOptions.drawerLabelStyle}
          activeTintColor={drawerNavigationOptions.drawerActiveTintColor}
          activeBackgroundColor={drawerNavigationOptions.drawerActiveBackgroundColor}
          inactiveTintColor={drawerNavigationOptions.drawerInactiveTintColor}
          inactiveBackgroundColor={drawerNavigationOptions.drawerInactiveBackgroundColor}
        />
      </DrawerContentScrollView>
      <View style={styles.spacer} />
      <View style={styles.drawerFooter}>
        <Text
          selectable
          style={[Typography.base, Typography.centered, Typography.tiny, Typography.inactive]}>
          {Constants?.expoConfig?.extra?.commitHash}
        </Text>
      </View>
    </View>
  );
};

/**
 * Navigation when connected to a lao
 */
const LaoNavigation: React.FC<unknown> = () => {
  const passedScreens = LaoHooks.useLaoNavigationScreens();

  // add the organizer or attendee screen depeding on the user
  const screens: LaoFeature.LaoScreen[] = useMemo(() => {
    return (
      [
        ...passedScreens,
        {
          id: STRINGS.navigation_lao_invite,
          Icon: makeIcon('invite'),
          Component: InviteScreen,
          headerShown: true,
          headerRight: NavigationPadding,
          order: -1,
        } as LaoFeature.LaoScreen,
        {
          id: STRINGS.navigation_lao_events,
          Icon: makeIcon('event'),
          Component: EventsNavigation,
          headerShown: false,
          order: 0,
          tabBarVisible: (routeName) =>
            // only show the tab bar if we are on the home events screen, not if we are
            // in a detail screen
            routeName === undefined || routeName === STRINGS.navigation_lao_events_home,
        } as LaoFeature.LaoScreen,
        // sort screens by order before rendering them
      ]
        .sort((a, b) => a.order - b.order)
        // wrap each screen component
        .map((s) => ({ ...s, Component: wrapWithOfflineHeader(s.Component) }))
    );
  }, [passedScreens]);

  return (
    <NoCurrentLaoErrorBoundary>
      <LaoNavigator.Navigator
        initialRouteName={STRINGS.navigation_lao_events}
        screenOptions={drawerNavigationOptions}
        drawerContent={LaoDrawerContent}>
        {screens.map(
          ({
            id,
            title,
            headerTitle,
            Component,
            headerShown,
            headerLeft,
            headerRight,
            Icon,
            tabBarVisible,
            testID,
          }) => (
            <LaoNavigator.Screen
              key={id}
              name={id}
              component={Component}
              options={({ route }) => {
                const routeName = getFocusedRouteNameFromRoute(route);

                return {
                  title: title || id,
                  headerTitle: headerTitle || title || id,
                  headerLeft: headerLeft || drawerNavigationOptions.headerLeft,
                  headerRight: headerRight || drawerNavigationOptions.headerRight,
                  drawerIcon: Icon,
                  headerShown,
                  tabBarTestID: testID,
                  tabBarStyle:
                    tabBarVisible && !tabBarVisible(routeName) ? { display: 'none' } : undefined,
                };
              }}
            />
          ),
        )}
      </LaoNavigator.Navigator>
    </NoCurrentLaoErrorBoundary>
  );
};

export default LaoNavigation;

export const LaoNavigationAppScreen: AppScreen = {
  id: STRINGS.navigation_app_lao,
  Component: LaoNavigation,
};
