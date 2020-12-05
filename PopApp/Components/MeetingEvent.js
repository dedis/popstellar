import React from 'react';
import {
  StyleSheet, View, Text, FlatList,
} from 'react-native';

import EventItem from './EventItem';
import { Spacing } from '../Styles';
import PROPS_TYPE from '../res/Props';

/**
 * Meeting component: a name text, a start time, an optionnal end time, an optionnal location
 *  a nested list of event
 *
 * TODO connect the field to the data receive by the organizer server
*/
const styles = StyleSheet.create({
  view: {
    marginHorizontal: Spacing.s,
    borderWidth: 1,
    borderRadius: 5,
    paddingHorizontal: Spacing.xs,
    marginBottom: Spacing.xs,
  },
  text: {
  },
  flatList: {
    marginTop: Spacing.xs,
  },
});

const MeetingEvent = ({ event }) => (
  <View style={styles.view}>
    <Text style={styles.text}>{event.name}</Text>
    <Text style={styles.text}>Start time</Text>
    <Text style={styles.text}>Optionnal end time</Text>
    <Text style={styles.text} dataDetectorType="link">Location, test.com</Text>
    <FlatList
      data={event.childrens}
      keyExtractor={(item) => item.id.toString()}
      renderItem={({ item }) => <EventItem event={item} />}
      listKey={event.id.toString()}
      style={styles.flatList}
    />
  </View>
);

MeetingEvent.propTypes = {
  event: PROPS_TYPE.event.isRequired,
};

export default MeetingEvent;
