import { CompositeScreenProps, useNavigation } from '@react-navigation/core';
import { StackScreenProps } from '@react-navigation/stack';
import { ListItem } from '@rneui/themed';
import React, { useEffect, useMemo, useState } from 'react';
import { View } from 'react-native';
import { useSelector } from 'react-redux';

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

  return (
    <View>
      {upcomingEvents.length > 0 && (
        <ListItem
          containerStyle={List.getListItemStyles(true, true)}
          style={List.getListItemStyles(true, true)}
          bottomDivider
          onPress={() =>
            navigation.push(STRINGS.navigation_app_lao, {
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
