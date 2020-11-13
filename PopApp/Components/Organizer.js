import React from 'react';
import { StyleSheet, View } from 'react-native';
import data from '../res/EventData';

import { Typography } from '../Styles';
import EventsCollapsableList from './EventsCollapsableList';

/**
* The Organizer component
*
* Manage the Organizer screen
*/
const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
  },
  text: {
    ...Typography.base,
  },
});

const Organizer = () => (
  <View style={styles.container}>
    <EventsCollapsableList data={data.filter((e) => e.title !== '')} closedList={['Future']} />
  </View>
);

export default Organizer;
