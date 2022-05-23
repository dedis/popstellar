import { useNavigation } from '@react-navigation/native';
import React, { useCallback, useMemo } from 'react';
import { SectionList, StyleSheet, Text, TextStyle, View, ViewStyle } from 'react-native';
import { useSelector } from 'react-redux';

import { TextBlock } from 'core/components';
import { Timestamp } from 'core/objects';
import { Spacing, Typography } from 'core/styles';
import STRINGS from 'resources/strings';

import { EventHooks } from '../hooks';
import { EventState } from '../objects';
import { makeEventListSelector } from '../reducer';
import Event from './Event';

const styles = StyleSheet.create({
  flexBox: {
    flex: 1,
    flexDirection: 'row',
    justifyContent: 'space-between',
  } as ViewStyle,
  buttonMatcher: {
    ...Typography.baseCentered,
    paddingLeft: Spacing.m,
    opacity: 0,
  } as TextStyle,
  expandButton: {
    ...Typography.baseCentered,
    paddingRight: Spacing.m,
  } as TextStyle,
});

const categorizeEventsByTime = (time: number, events: EventState[]) => {
  const pastEvents: EventState[] = [];
  const currentEvents: EventState[] = [];
  const futureEvents: EventState[] = [];

  events.forEach((e: EventState) => {
    if ((e.end && e.end < time) || (!e.end && e.start < time)) {
      pastEvents.push(e);
      return;
    }
    if (e.start > time) {
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
  const laoId = EventHooks.useCurrentLaoId();

  if (!laoId) {
    throw new Error('Cannot show an event list if you are not connected to a lao!');
  }

  const eventListSelector = useMemo(() => makeEventListSelector(laoId.valueOf()), [laoId]);
  const events = useSelector(eventListSelector);
  const [pastEvents, currentEvents, futureEvents] = categorizeEventsByTime(
    Timestamp.EpochNow().valueOf(),
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

  const isOrganizer = EventHooks.useIsLaoOrganizer();

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
      renderItem={({ item }) => <Event eventId={item.id} eventType={item.eventType} />}
      renderSectionHeader={({ section: { title } }) => renderSectionHeader(title)}
    />
  );
};

export default EventList;
