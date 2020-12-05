import React from 'react';
import {
  StyleSheet, View,
} from 'react-native';

import eventsData from '../res/EventData'; // fake data to show how the component works
import EventsCollapsableList from './EventsCollapsableList';

/**
 * Manage the Attendee screen: A section list of propreties and events
 *
 * The section list show the events and propreties of the LAO open in
 * the organitation UI.
 *
 * By default only the past and present section are open.
 *
 * TODO use the data receive by the organization server
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
