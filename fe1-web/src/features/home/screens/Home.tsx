import { CompositeScreenProps, useNavigation } from '@react-navigation/core';
import { StackScreenProps } from '@react-navigation/stack';
import React, { FunctionComponent, useEffect, useMemo } from 'react';
import { Text } from 'react-native';
import { useDispatch } from 'react-redux';

import { PoPIcon } from 'core/components';
import PoPTouchableOpacity from 'core/components/PoPTouchableOpacity';
import ScreenWrapper from 'core/components/ScreenWrapper';
import { useActionSheet } from 'core/hooks/ActionSheet';
import { AppParamList } from 'core/navigation/typing/AppParamList';
import { HomeParamList } from 'core/navigation/typing/HomeParamList';
import { Color, Icon, Typography } from 'core/styles';
import STRINGS from 'resources/strings';

import { HomeHooks } from '../hooks';

type NavigationProps = CompositeScreenProps<
  StackScreenProps<HomeParamList, typeof STRINGS.navigation_home_home>,
  StackScreenProps<AppParamList, typeof STRINGS.navigation_app_home>
>;

/**
 * Manage the Home screen component: if the user is not connected to any LAO, a welcome message
 * is displayed, otherwise a list available previously connected LAOs is displayed instead
 */
const Home: FunctionComponent<unknown> = () => {
  const navigation = useNavigation<NavigationProps['navigation']>();
  const laos = HomeHooks.useLaoList();
  const LaoList = HomeHooks.useLaoListComponent();

  const laoId = HomeHooks.useCurrentLaoId();
  const disconnectFromLao = HomeHooks.useDisonnectFromLao();

  useEffect(() => {
    // Return the function to unsubscribe from the event so it gets removed on unmount
    return navigation.addListener('focus', () => {
      // The screen is now focused, check if we are connected to a lao
      if (laoId) {
        // if we enter this screen and connected to a lao
        // disconnect from this lao
        disconnectFromLao();
      }
    });
  }, [navigation, laoId, disconnectFromLao]);

  const toolbarItems = useMemo(
    () => [
      {
        title: STRINGS.home_create_lao,
        onPress: () =>
          navigation.navigate(STRINGS.navigation_home_connect, {
            screen: STRINGS.navigation_connect_launch,
          }),
      },
      {
        title: STRINGS.home_join_lao,
        onPress: () =>
          navigation.navigate(STRINGS.navigation_home_connect, {
            screen: STRINGS.navigation_connect_scan,
          }),
      },
    ],
    [navigation],
  );

  return laos && laos.length > 0 ? (
    <ScreenWrapper toolbarItems={toolbarItems}>
      <LaoList />
    </ScreenWrapper>
  ) : (
    <ScreenWrapper toolbarItems={toolbarItems}>
      <Text style={Typography.heading}>{STRINGS.home_setup_heading}</Text>
      <Text style={Typography.paragraph}>{STRINGS.home_setup_description_1}</Text>
      <Text style={Typography.paragraph}>{STRINGS.home_setup_description_2}</Text>
    </ScreenWrapper>
  );
};

export default Home;

/**
 * Component rendered in the top right of the navigation bar
 * Shows three dots allowing the user to log out of the application
 * and in the future possibly access app settings
 */
export const HomeHeaderRight = () => {
  const navigation = useNavigation<NavigationProps['navigation']>();
  const showActionSheet = useActionSheet();
  const forgetSeed = HomeHooks.useForgetSeed();
  const dispatch = useDispatch();

  return (
    <PoPTouchableOpacity
      onPress={() =>
        showActionSheet([
          {
            displayName: STRINGS.home_logout,
            action: () => {
              forgetSeed();
              navigation.navigate(STRINGS.navigation_app_wallet_create_seed);
            },
          },
          {
            displayName: STRINGS.home_logout_clear_data,
            action: () => {
              dispatch({ type: 'CLEAR_STORAGE', value: {} });
              navigation.navigate(STRINGS.navigation_app_wallet_create_seed);
            },
          },
        ])
      }>
      <PoPIcon name="options" color={Color.inactive} size={Icon.size} />
    </PoPTouchableOpacity>
  );
};
