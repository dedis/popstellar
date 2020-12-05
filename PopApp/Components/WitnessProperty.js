import React from 'react';
import {
  StyleSheet, View, Text, FlatList,
} from 'react-native';

import { Spacing } from '../Styles';
import PROPS_TYPE from '../res/Props';
import STRINGS from '../res/strings';

/**
 * Witnesses property component: name of the proprety, list of witnesses
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
    <Text style={styles.text}>{STRINGS.witness_name}</Text>
    <FlatList
      data={event.witnesses}
      keyExtractor={(item) => item}
      renderItem={({ item }) => <Text>{item}</Text>}
      listKey={event.id.toString()}
      style={styles.flatList}
    />
  </View>
);

MeetingEvent.propTypes = {
  event: PROPS_TYPE.event.isRequired,
};

export default MeetingEvent;
