import React from 'react';
import {
  StyleSheet, View, Text, FlatList,
} from 'react-native';

import STRINGS from '../res/strings';
import { Spacing, Typography } from '../Styles';
import LAOItem from './LAOItem';

import LAO from '../res/laoData';

/**
* The Home component
*
* Manage the Home screen
*/
const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
  },
  text: {
    ...Typography.important,
  },
  flatList: {
    marginTop: Spacing.s,
  },
});

const render = () => {
  if (!LAO || !LAO.length) {
    <View style={styles.container}>
      <Text style={styles.text}>{STRINGS.home_welcome}</Text>
      <Text style={styles.text}>{STRINGS.home_connect_lao}</Text>
      <Text style={styles.text}>{STRINGS.home_launch_lao}</Text>
    </View>;
  } else {
    <View style={styles.container}>
      <FlatList
        data={LAO}
        keyExtractor={(item) => item.id.toString()}
        renderItem={({ item }) => <LAOItem LAO={item} navigation={this.props.navigation} />}
        style={styles.flatList}
      />
    </View>;
  }
};

const Home = () => (
  render()
);

export default Home;
