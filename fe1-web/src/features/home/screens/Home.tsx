import React from 'react';
import { View } from 'react-native';

import { TextBlock } from 'core/components';
import containerStyles from 'core/styles/stylesheets/containerStyles';
import { LaoList } from 'features/lao/components';
import { useLaoList } from 'features/lao/hooks';
import STRINGS from 'resources/strings';

/**
 * Manage the Home screen component: if the user is not connected to any LAO, a welcome message
 * is displayed, otherwise a list available previously connected LAOs is displayed instead
 */
const Home = () => {
  const laos = useLaoList();
  return laos && laos.length > 0 ? (
    <LaoList />
  ) : (
    <View style={containerStyles.centered}>
      <TextBlock bold text={STRINGS.home_welcome} />
      <TextBlock bold text={STRINGS.home_connect_lao} />
      <TextBlock bold text={STRINGS.home_launch_lao} />
    </View>
  );
};

export default Home;
