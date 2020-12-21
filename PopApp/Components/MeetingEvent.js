import React from 'react';
import {
  StyleSheet, View, Text, FlatList,
} from 'react-native';

import EventItem from './EventItem';
import { Spacing } from '../Styles';
import PROPS_TYPE from '../res/Props';

/**
 * Meeting component
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

const MeetingEvent = ({ event }) => {
  const dateToString = (timestamp) => {
    const d = new Date(timestamp * 1000);
    return `${d.getDate()}-${d.getMonth() + 1}-${d.getFullYear()} `
    + `${d.getHours() < 10 ? 0 : ''}${d.getHours()}:`
    + `${d.getMinutes() < 10 ? 0 : ''}${d.getMinutes()}:${d.getSeconds()}`;
  };

  return (
    <View style={styles.view}>
      <Text style={styles.text}>{event.name}</Text>
      <Text style={styles.text}>{`Start at ${dateToString(event.start)}`}</Text>
      {event.end && <Text style={styles.text}>{`End at ${dateToString(event.end)}`}</Text>}
      {event.location && event.location.trim() !== ''
        && <Text style={styles.text} dataDetectorType="link">{event.location}</Text>}
      <FlatList
        data={event.childrens}
        keyExtractor={(item) => item.id.toString()}
        renderItem={({ item }) => <EventItem event={item} />}
        listKey={event.id.toString()}
        style={styles.flatList}
      />
    </View>
  );
};

MeetingEvent.propTypes = {
  event: PROPS_TYPE.meeting.isRequired,
};

export default MeetingEvent;
