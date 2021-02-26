/* eslint-disable */
import React from 'react';
import {
  StyleSheet, View, Text, FlatList, TextInput, Button,
} from 'react-native';

import EventItem from './EventItem';
import { Spacing } from '../styles';
import PROPS_TYPE from '../res/Props';

// Fake data to simulate the functionality of the component
const QUESTION = [
  {
    id: 1,
    tile: 'Question 1',
    score: 4,
  },
  {
    id: 2,
    tile: 'Discussion 1',
    score: 1,
  },
  {
    id: 3,
    tile: 'Question 2',
    score: 0,
  },
];

/**
 * Discussion component: a name string, a list of questions,
 * if open a text input for send question, and a list of nested events
 *
 * TODO must be modify when the discussion event will be describe in UI Specifications
 * TODO the send question button must send the question to the organizer server
 * TODO the data and the open state must be ask to the organizer server
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

// @ts-ignore
const DiscussionEvent = ({ event }) => (
  <View style={styles.view}>
    <Text style={styles.text}>{event.name}</Text>
    <FlatList
      data={QUESTION}
      keyExtractor={(item) => item.id.toString()}
      renderItem={({ item }) => (
        <View style={{ flexDirection: 'row', justifyContent: 'space-between' }}>
          <Text style={styles.text}>{item.tile}</Text>
          <Text style={styles.text}>{item.score === 0 ? '' : item.score}</Text>
        </View>
      )}
      listKey={`${event.id.toString()}Disccusion`}
    />
    <Text style={styles.text}>If discussion open</Text>
    <View style={{ flexDirection: 'row', marginBottom: Spacing.xs }}>
      <TextInput style={{ flex: 1 }} placeholder="Your question" />
      { /* <Button title="Send" /> */ }
    </View>
    <FlatList
      data={event.children}
      keyExtractor={(item) => item.id.toString()}
      // @ts-ignore
      renderItem={({ item }) => <EventItem event={item} />}
      listKey={`DiscussionEvent-${event.id.toString()}`}
      style={styles.flatList}
    />
  </View>
);

DiscussionEvent.propTypes = {
  event: PROPS_TYPE.event.isRequired,
};

export default DiscussionEvent;
