import React from 'react';
import {
  StyleSheet, View,
} from 'react-native';

import eventsData from '../res/EventData';
import EventsCollapsableList from './EventsCollapsableList';

/**
* The Attendee component
*
* Manage the Attendee screen
*/
const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
  },
});

const Attendee = () => (
  <View style={styles.container}>
    <EventsCollapsableList
      data={eventsData}
      closedList={['Future', '']}
    />
  </View>
);

export default Attendee;
