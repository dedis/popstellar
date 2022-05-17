import React, { FunctionComponent } from 'react';
import { Text, View } from 'react-native';

import ScreenWrapper from 'core/components/ScreenWrapper';
import { Typography } from 'core/styles';
import containerStyles from 'core/styles/stylesheets/containerStyles';

import { HomeHooks } from '../hooks';

/**
 * Manage the Home screen component: if the user is not connected to any LAO, a welcome message
 * is displayed, otherwise a list available previously connected LAOs is displayed instead
 */
const Home: FunctionComponent = () => {
  const laos = HomeHooks.useLaoList();
  const LaoList = HomeHooks.useLaoListComponent();
  console.log('h');

  return laos && laos.length > 0 ? (
    <ScreenWrapper>
      <LaoList />
    </ScreenWrapper>
  ) : (
    <ScreenWrapper>
      <View style={containerStyles.centeredY}>
        <Text style={Typography.heading}>h</Text>
      </View>
    </ScreenWrapper>
  );
};

export default Home;
