import React, { FunctionComponent } from 'react';
import { View } from 'react-native';

import { TextBlock } from 'core/components';
import ScreenWrapper from 'core/components/ScreenWrapper';
import containerStyles from 'core/styles/stylesheets/containerStyles';
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
      <View style={containerStyles.centeredY}>
        <TextBlock bold text={STRINGS.home_welcome} />
        <TextBlock bold text={STRINGS.home_connect_lao} />
        <TextBlock bold text={STRINGS.home_launch_lao} />
      </View>
    </ScreenWrapper>
  );
};

export default Home;
