import React from 'react';
import { StyleSheet, View, FlatList } from 'react-native';
import { useSelector } from 'react-redux';

import { makeLaosList } from 'store/reducers';
import { Lao } from 'model/objects';

import LAOItem from 'components/LAOItem';
import TextBlock from 'components/TextBlock';

import { Spacing } from 'styles/index';
import styleContainer from 'styles/stylesheets/container';
import STRINGS from 'res/strings';

/**
 * Manage the Home screen component: if the user is not connected to any LAO, a welcome message
 * is displayed, otherwise a list available previously connected LAOs is displayed instead
*/
const styles = StyleSheet.create({
  flatList: {
    marginTop: Spacing.s,
  },
});

function getWelcomeMessageDisplay() {
  return (
    <View style={styleContainer.centered}>
      <TextBlock bold text={STRINGS.home_welcome} />
      <TextBlock bold text={STRINGS.home_connect_lao} />
      <TextBlock bold text={STRINGS.home_launch_lao} />
    </View>
  );
}

function getConnectedLaosDisplay(laos: Lao[]) {
  return (
    <View style={styleContainer.centered}>
      <FlatList
        data={laos}
        keyExtractor={(item) => item.id.toString()}
        renderItem={({ item }) => <LAOItem LAO={item} />}
        style={styles.flatList}
      />
    </View>
  );
}

const Home = () => {
  const laosList = makeLaosList();
  const laos = useSelector(laosList);

  if (laos !== undefined && !laos.length) {
    return getConnectedLaosDisplay(laos);
  }
  return getWelcomeMessageDisplay();
};

export default Home;
