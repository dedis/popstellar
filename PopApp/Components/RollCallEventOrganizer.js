import React from 'react';
import {
  StyleSheet, View, Text, FlatList, Button,
} from 'react-native';
import { useNavigation } from '@react-navigation/native';

import EventItem from './EventItem';
import { Buttons, Spacing } from '../Styles';
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
  buttons: {
    ...Buttons.base,
  },
});

const RollCallEventOrganizer = ({ event }) => {
  const navigation = useNavigation();

  return (
    <View style={styles.view}>
      <Text style={styles.text}>Status (Future, Open or Closed)</Text>
      <Text style={styles.text}>Participants #</Text>
      <View style={styles.buttons}>
        <Button title="Open" onPress={() => navigation.navigate('Roll-Call')} />
      </View>
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

RollCallEventOrganizer.propTypes = {
  event: PROPS_TYPE.event.isRequired,
};

export default RollCallEventOrganizer;
