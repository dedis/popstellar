import React from 'react';
import {
  StyleSheet, View, Text, FlatList,
} from 'react-native';

import EventItem from './EventItem';
import { Spacing } from '../Styles';
import PROPS_TYPE from '../res/Props';

/**
 * Roll-call component
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

const RollCallEvent = ({ event }) => (
  <View style={styles.view}>
    <Text style={styles.text}>Status (Future, Open or Closed)</Text>
    <Text style={styles.text}>Participants #</Text>
    <Text style={styles.text}>If non organizer : QR code</Text>
    {/* <Text style={styles.text}>If organizer</Text>
    <CameraButton action={() => {}} /> */}
    <FlatList
      data={event.childrens}
      keyExtractor={(item) => item.id.toString()}
      renderItem={({ item }) => <EventItem event={item} />}
      listKey={event.id.toString()}
      style={styles.flatList}
    />
  </View>
);

RollCallEvent.propTypes = {
  event: PROPS_TYPE.event.isRequired,
};

export default RollCallEvent;
