import React from 'react';
import { StyleSheet, View, FlatList } from 'react-native';
import { useSelector } from 'react-redux';

import { makeLaosList } from 'store';
import { Lao } from 'model/objects';

import { Spacing } from 'styles';
import containerStyles from 'styles/stylesheets/containerStyles';
import STRINGS from 'res/strings';

import LAOItem from 'components/LAOItem';
import TextBlock from 'components/TextBlock';

/**
 * Manage the Home screen component: if the user is not connected to any LAO, a welcome message
 * is displayed, otherwise a list available previously connected LAOs is displayed instead
 *
 * TODO use the list that the user have already connect to, and ask data to
 *  some organizer server if needed
 */
const styles = StyleSheet.create({
  flatList: {
    marginTop: Spacing.s,
  },
});

// FIXME: define interface + types, requires availableLaosReducer to be migrated first
function getConnectedLaosDisplay(laos: Lao[]) {
  return (
    <View style={containerStyles.centered}>
      <FlatList
        data={laos}
        keyExtractor={(item) => item.id.toString()}
        renderItem={({ item }) => <LAOItem LAO={item} />}
        style={styles.flatList}
      />
    </View>
  );
}

function getWelcomeMessageDisplay() {
  return (
    <View style={containerStyles.centered}>
      <TextBlock bold text={STRINGS.home_welcome} />
      <TextBlock bold text={STRINGS.home_connect_lao} />
      <TextBlock bold text={STRINGS.home_launch_lao} />
    </View>
  );
}

const Home = () => {
  const laosList = makeLaosList();
  const laos: Lao[] = useSelector(laosList);

  return laos && !laos.length ? getConnectedLaosDisplay(laos) : getWelcomeMessageDisplay();
};

export default Home;
