import { CompositeScreenProps, useNavigation } from '@react-navigation/core';
import { StackScreenProps } from '@react-navigation/stack';
import { ListItem } from '@rneui/themed';
import React, { useEffect, useMemo, useState } from 'react';
import { Text, View } from 'react-native';
import { useSelector } from 'react-redux';
import ReactTimeago from 'react-timeago';

import { AppParamList } from 'core/navigation/typing/AppParamList';
import { LaoEventsParamList } from 'core/navigation/typing/LaoEventsParamList';
import { LaoParamList } from 'core/navigation/typing/LaoParamList';
import { Timestamp } from 'core/objects';
import { List, Typography } from 'core/styles';
import STRINGS from 'resources/strings';

import { categorizeEventsByTime } from '../functions';
import { EventHooks } from '../hooks';
import { EventState } from '../objects';
import { makeEventListSelector } from '../reducer';
import EventList from './EventList';

type NavigationProps = CompositeScreenProps<
  StackScreenProps<LaoEventsParamList, typeof STRINGS.navigation_lao_events_home>,
  CompositeScreenProps<
    StackScreenProps<LaoParamList, typeof STRINGS.navigation_lao_events>,
    StackScreenProps<AppParamList, typeof STRINGS.navigation_app_lao>
  >
>;

/**
 * Collapsible list of events: list with 3 sections corresponding
 * to 'past', 'present' and 'future' events.
 */
const EventLists = () => {
  const laoId = EventHooks.useCurrentLaoId();
  const navigation = useNavigation<NavigationProps['navigation']>();

  const eventListSelector = useMemo(() => makeEventListSelector(laoId), [laoId]);
  const events = useSelector(eventListSelector);

  const isOrganizer = EventHooks.useIsLaoOrganizer();

  const [{ pastEvents, currentEvents, upcomingEvents }, setEvents] = useState<{
    pastEvents: EventState[];
    currentEvents: EventState[];
    upcomingEvents: EventState[];
  }>(() => categorizeEventsByTime(Timestamp.EpochNow(), events));

  useEffect(() => {
    const interval = setInterval(
      () => setEvents(categorizeEventsByTime(Timestamp.EpochNow(), events)),
      1000,
    );

    // clear the interval when unmouting the component
    return () => clearInterval(interval);
  }, [events]);

  // find upcoming event that is nearest / closest in the future
  const closestUpcomingEvent = useMemo(
    () =>
      upcomingEvents.reduce<EventState | null>((closestEvent, event) => {
        if (closestEvent === null || event.start < closestEvent.start) {
          return event;
        }

        return closestEvent;
      }, null),
    [upcomingEvents],
  );

  return events.length === 0 ? (
    // if no events, display a welcome message on the screen
    <View>
      <Text style={[Typography.base, Typography.centered]}>
        {isOrganizer ? STRINGS.events_welcome_organizer : STRINGS.events_welcome_attendee}
      </Text>
    </View>
  ) : (
    <View>
      {upcomingEvents.length > 0 && closestUpcomingEvent && (
        <ListItem
          containerStyle={List.getListItemStyles(true, true)}
          style={List.getListItemStyles(true, true)}
          bottomDivider
          onPress={() =>
            navigation.navigate(STRINGS.navigation_app_lao, {
              screen: STRINGS.navigation_lao_events,
              params: {
                screen: STRINGS.navigation_lao_events_upcoming,
              },
            })
          }>
          <ListItem.Content>
            <ListItem.Title style={Typography.base}>
              {STRINGS.events_upcoming_events}
            </ListItem.Title>
            <ListItem.Subtitle style={Typography.small}>
              {STRINGS.events_closest_upcoming_event}{' '}
              <ReactTimeago live date={closestUpcomingEvent.start * 1000} />
            </ListItem.Subtitle>
          </ListItem.Content>
          <ListItem.Chevron />
        </ListItem>
      )}

      <View style={List.container}>
        <EventList title={STRINGS.events_list_current} isDefaultExpanded events={currentEvents} />
        <EventList
          title={STRINGS.events_list_past}
          isDefaultExpanded={currentEvents.length === 0}
          events={pastEvents}
        />
      </View>
    </View>
  );
};

export default EventLists;
