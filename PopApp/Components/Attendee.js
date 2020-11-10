import React from 'react';
import {
  StyleSheet, View,
} from 'react-native';

import { Typography, Spacing } from '../Styles';
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
  text: {
    ...Typography.base,
  },
  textItem: {
    borderWidth: 1,
    borderRadius: 5,
    marginBottom: Spacing.xs,
    paddingHorizontal: Spacing.xs,
  },
  touchable: {
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
});

const Attendee = () => (
  <View style={[styles.container, { marginHorizontal: Spacing.xs }]}>
    <EventsCollapsableList
      data={eventsData}
      closedList={['Future', '']}
    />
  </View>
);

export default Attendee;
