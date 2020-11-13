import React from 'react';
import {
  StyleSheet, View, Text, FlatList,
} from 'react-native';

import { Spacing } from '../Styles';
import PROPS_TYPE from '../res/Props';
import MeetingEvent from './MeetingEvent';
import DiscussionEvent from './DiscussionEvent';
import PollEvent from './PollEvent';
import RollCallEvent from './RollCallEvent';

/**
* The Event item component
*
*/
const styles = StyleSheet.create({
  view: {
    marginHorizontal: Spacing.s,
  },
  text: {
    borderWidth: 1,
    borderRadius: 5,
    paddingHorizontal: Spacing.xs,
    marginBottom: Spacing.xs,
  },
});

const EventItem = ({ event }) => {
  switch (event.type) {
    case 'meeting':
      return (<MeetingEvent event={event} />);
    case 'rollCall':
      return (<RollCallEvent event={event} />);
    case 'poll':
      return (<PollEvent event={event} />);
    case 'discussion':
      return (<DiscussionEvent event={event} />);
    default:
      return (
        <View style={styles.view}>
          <Text style={styles.text}>{event.name}</Text>
          <FlatList
            data={event.childrens}
            keyExtractor={(item) => item.id.toString()}
            renderItem={({ item }) => <EventItem event={item} />}
            listKey={event.id.toString()}
          />
        </View>
      );
  }
};

EventItem.propTypes = {
  event: PROPS_TYPE.event.isRequired,
};

export default EventItem;
