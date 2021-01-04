import React, { useState } from 'react';
import {
  StyleSheet, View, Text, TouchableOpacity, FlatList,
} from 'react-native';
import PropTypes from 'prop-types';

import { Typography, Spacing } from '../Styles';
import EventItem from './EventItem';
import PROPS_TYPE from '../res/Props';

/**
 * Manage the collapsable list of events: contain a section list of event
 *
 * By default all sections are open, you can set the closed section by putting their names in the
 *  closedList
 *
 * It is assume that the nested events are already been calculated.
 * They shoud be in the childrens value of the event
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

/**
 * List of events of the same section
 */
const Item = ({ events, closedList }) => {
  const [open, setopen] = closedList.includes(events.title) ? useState(false) : useState(true);
  const onPress = () => {
    setopen(!open);
  };
  return (
    <View style={styles.container}>
      <TouchableOpacity
        onPress={onPress}
        activeOpacity={1}
        style={styles.touchable}
      >
        <Text style={styles.text}>{events.title}</Text>
        <Text style={styles.text}>{open ? '⌵' : 'ᐳ'}</Text>
      </TouchableOpacity>
      {open && (
      <FlatList
        data={events.data}
        keyExtractor={(item) => item.id.toString()}
        renderItem={({ item }) => <EventItem event={item} />}
        listKey={events.title}
      />
      )}
    </View>
  );
};

Item.propTypes = {
  events: PropTypes.shape({
    title: PropTypes.string.isRequired,
    data: PropTypes.arrayOf(
      PropTypes.oneOfType([PROPS_TYPE.event, PROPS_TYPE.property]),
    ).isRequired,
  }).isRequired,
  closedList: PropTypes.arrayOf(PropTypes.string).isRequired,
};

/**
 * List of all section of the data
 *
 * Data must have a title (String) and a data (List of event object) field
 */
const EventsCollapsableList = ({ data, closedList }) => (
  <FlatList
    data={data}
    keyExtractor={(item) => item.title}
    renderItem={({ item }) => <Item events={item} closedList={closedList} />}
    listKey="Base"
  />
);

EventsCollapsableList.propTypes = {
  data: PropTypes.arrayOf(Item.propTypes.events).isRequired,
  closedList: PropTypes.arrayOf(PropTypes.string).isRequired,
};

export default EventsCollapsableList;
