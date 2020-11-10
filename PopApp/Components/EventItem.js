import React from 'react';
import {
  StyleSheet, View, Text, FlatList,
} from 'react-native';

import { Spacing } from '../Styles';
import PROPS_TYPE from '../res/Props';

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

const EventItem = ({ event }) => (
  <View style={styles.view}>
    <Text style={styles.text}>{event.name}</Text>
    <FlatList
      data={event.childrens}
      keyExtractor={(item) => item.id.toString()}
      renderItem={({ item }) => <EventItem event={item} />}
    />
  </View>
);

EventItem.propTypes = {
  event: PROPS_TYPE.event.isRequired,
};

export default EventItem;
