import { useNavigation } from '@react-navigation/core';
import React, { FunctionComponent, useEffect, useState } from 'react';
import { StyleSheet, Text, TextStyle, View } from 'react-native';

import ScreenWrapper from 'core/components/ScreenWrapper';
import { Typography } from 'core/styles';
import containerStyles from 'core/styles/stylesheets/containerStyles';
import STRINGS from 'resources/strings';

import { HomeHooks } from '../hooks';

const styles = StyleSheet.create({
  text: {
    ...Typography.base,
  } as TextStyle,
});

/**
 * Manage the Home screen component: if the user is not connected to any LAO, a welcome message
 * is displayed, otherwise a list available previously connected LAOs is displayed instead
 */
const Home: FunctionComponent = () => {
  const laos = HomeHooks.useLaoList();
  const LaoList = HomeHooks.useLaoListComponent();

  const hasSeed = HomeHooks.useHasSeed();

  return laos && laos.length > 0 ? (
    <ScreenWrapper>
      <LaoList />
    </ScreenWrapper>
  ) : (
    <ScreenWrapper>
      <View style={containerStyles.centeredY}>
        <Text style={Typography.heading}>{STRINGS.home_welcome}</Text>
        <Text style={Typography.paragraph}>{STRINGS.home_description}</Text>
        {hasSeed ? (
          <Text style={Typography.paragraph}>{STRINGS.home_wallet_setup}</Text>
        ) : (
          <Text style={Typography.paragraph}>{STRINGS.home_wallet}</Text>
        )}
      </View>
    </ScreenWrapper>
  );
};

export default Home;
