import { useNavigation } from '@react-navigation/native';
import React, { useCallback } from 'react';
import { SectionList, StyleSheet, Text, TextStyle, View, ViewStyle } from 'react-native';
import { useSelector } from 'react-redux';

import { TextBlock } from 'core/components';
import { Timestamp } from 'core/objects';
import { Spacing, Typography } from 'core/styles';
import STRINGS from 'resources/strings';

import { EventsHooks } from '../hooks';
import { LaoEvent } from '../objects';
import { selectEventsList } from '../reducer';
import Event from './Event';

const styles = StyleSheet.create({
  flexBox: {
    flex: 1,
    flexDirection: 'row',
    justifyContent: 'space-between',
  } as ViewStyle,
  buttonMatcher: {
    ...Typography.base,
    paddingLeft: Spacing.m,
    opacity: 0,
  } as TextStyle,
  expandButton: {
    ...Typography.base,
    paddingRight: Spacing.m,
  } as TextStyle,
});

const categorizeEventsByTime = (time: Timestamp, events: LaoEvent[]) => {
  const pastEvents: LaoEvent[] = [];
  const currentEvents: LaoEvent[] = [];
  const futureEvents: LaoEvent[] = [];

  events.forEach((e: LaoEvent) => {
    if ((e.end && e.end.before(time)) || (!e.end && e.start.before(time))) {
      pastEvents.push(e);
      return;
    }
    if (e.start.after(time)) {
      futureEvents.push(e);
      return;
    }
    currentEvents.push(e);
  });

  return [pastEvents, currentEvents, futureEvents];
};

/**
 * Collapsible list of events: list with 3 sections corresponding
 * to 'past', 'present' and 'future' events.
 *
 * Nested events should be in the children value of the parent event.
 */
const EventList = () => {
  const events = useSelector(selectEventsList);
  const [pastEvents, currentEvents, futureEvents] = categorizeEventsByTime(
    Timestamp.EpochNow(),
    events,
  );

  const data = [
    {
      title: 'Past',
      data: pastEvents,
    },
    {
      title: 'Present',
      data: currentEvents,
    },
    {
      title: 'Future',
      data: futureEvents,
    },
  ];

  const isOrganizer = EventsHooks.useIsLaoOrganizer();

  // FIXME: use proper navigation type
  const navigation = useNavigation<any>();

  const renderSectionHeader = useCallback(
    (title: string) => {
      const sectionTitle = <TextBlock bold text={title} />;
      const expandSign: string = '+';

      return isOrganizer && title === 'Future' ? (
        <View style={styles.flexBox}>
          <Text style={styles.buttonMatcher}>{expandSign}</Text>
          {sectionTitle}
          <Text
            style={styles.expandButton}
            onPress={() => navigation.navigate(STRINGS.organizer_navigation_tab_create_event, {})}>
            {expandSign}
          </Text>
        </View>
      ) : (
        sectionTitle
      );
    },
    [isOrganizer, navigation],
  );

  return (
    <SectionList
      sections={data}
      keyExtractor={(item) => item.id.valueOf()}
      renderItem={({ item }) => <Event event={item} />}
      renderSectionHeader={({ section: { title } }) => renderSectionHeader(title)}
    />
  );
};

export default EventList;
