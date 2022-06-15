import { CompositeScreenProps, useNavigation } from '@react-navigation/core';
import { StackScreenProps } from '@react-navigation/stack';
import React, { FunctionComponent, useEffect } from 'react';
import { Text } from 'react-native';

import ScreenWrapper from 'core/components/ScreenWrapper';
import { AppParamList } from 'core/navigation/typing/AppParamList';
import { HomeParamList } from 'core/navigation/typing/HomeParamList';
import { Typography } from 'core/styles';
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
const Home: FunctionComponent = () => {
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

        // FIXME
        // In the best case we would just call the function here
        // unfortunately react-native-navigation sometimes still has lao screens loaded
        // resulting in the application crashing
        // setTimeout delays the function call but a this workaround is a bit brittle
        // the cleanest solution would probably be to navigate to this screen
        // if no an error indicating no active lao is thrown
        // react error boundaries might help achieving this
        setTimeout(disconnectFromLao, 1000);
      }
    });
  }, [navigation, laoId, disconnectFromLao]);

  return laos && laos.length > 0 ? (
    <ScreenWrapper>
      <LaoList />
    </ScreenWrapper>
  ) : (
    <ScreenWrapper>
      <Text style={Typography.heading}>{STRINGS.home_setup_heading}</Text>
      <Text style={Typography.paragraph}>{STRINGS.home_setup_description_1}</Text>
      <Text style={Typography.paragraph}>{STRINGS.home_setup_description_2}</Text>
    </ScreenWrapper>
  );
};

export default Home;
