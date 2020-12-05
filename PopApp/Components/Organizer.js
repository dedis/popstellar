import React from 'react';
import { StyleSheet, View } from 'react-native';
import data from '../res/EventData';

import { Typography } from '../Styles';
import OrganizerEventsCollapsableList from './OrganizerCollapsableList';

/**
 * Manage the Organizer screen: A section list of propreties and events
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
  text: {
    ...Typography.base,
  },
});

const Organizer = () => (
  <View style={styles.container}>
    <OrganizerEventsCollapsableList data={data} closedList={['Future', '']} />
  </View>
);

export default Organizer;
