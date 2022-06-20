import React, { FunctionComponent } from 'react';
import { Text } from 'react-native';

import ScreenWrapper from 'core/components/ScreenWrapper';
import { Typography } from 'core/styles';
import STRINGS from 'resources/strings';

import { HomeHooks } from '../hooks';

/**
 * Manage the Home screen component: if the user is not connected to any LAO, a welcome message
 * is displayed, otherwise a list available previously connected LAOs is displayed instead
 */
const Home: FunctionComponent = () => {
  const laos = HomeHooks.useLaoList();
  const LaoList = HomeHooks.useLaoListComponent();

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
