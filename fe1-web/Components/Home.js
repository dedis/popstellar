import React from 'react';
import {
  StyleSheet, View, Text, FlatList,
} from 'react-native';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';

import STRINGS from '../res/strings';
import { Spacing, Typography } from '../Styles';
import LAOItem from './LAOItem';
import PROPS_TYPE from '../res/Props';

/**
 * Manage the Home screen component: if the LOAs is empty a welcome string (divide in three string)
 * else a list of the name of all LAO in the LAOs
 *
 * TODO use the list that the user have already connect to, and ask data to
 *  some organizer server if needed
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

const Home = ({ LAOs }) => {
  if (!LAOs || !LAOs.length) {
    return (
      <View style={styles.container}>
        <Text style={styles.text}>{STRINGS.home_welcome}</Text>
        <Text style={styles.text}>{STRINGS.home_connect_lao}</Text>
        <Text style={styles.text}>{STRINGS.home_launch_lao}</Text>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <FlatList
        data={LAOs}
        keyExtractor={(item) => item.id.toString()}
        renderItem={({ item }) => <LAOItem LAO={item} />}
        style={styles.flatList}
      />
    </View>
  );
};

Home.propTypes = {
  LAOs: PropTypes.arrayOf(PROPS_TYPE.LAO).isRequired,
};

const mapStateToProps = (state) => ({
  LAOs: state.connectLAOsReducer.LAOs,
});

export default connect(mapStateToProps)(Home);
