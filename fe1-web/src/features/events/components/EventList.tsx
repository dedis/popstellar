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
import EventListItem from './EventListItem';

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
const EventList = () => {
  const laoId = EventHooks.useAssertCurrentLaoId();
  const navigation = useNavigation<NavigationProps['navigation']>();

  const eventListSelector = useMemo(() => makeEventListSelector(laoId.valueOf()), [laoId]);
  const events = useSelector(eventListSelector);

  const [{ pastEvents, currentEvents }, setEvents] = useState<{
    pastEvents: EventState[];
    currentEvents: EventState[];
    upcomingEvents: EventState[];
  }>(() => categorizeEventsByTime(Timestamp.EpochNow(), events));

  const [showCurrent, setShowCurrent] = useState(true);
  const [showPast, setShowPast] = useState(false);

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
          <ListItem.Title style={Typography.base}>{STRINGS.events_upcoming_events}</ListItem.Title>
        </ListItem.Content>
        <ListItem.Chevron />
      </ListItem>

      <View style={List.container}>
        <ListItem.Accordion
          containerStyle={List.accordionItem}
          style={List.accordionItem}
          content={
            <ListItem.Content>
              <ListItem.Title style={[Typography.base, Typography.important]}>
                {STRINGS.events_list_current}
              </ListItem.Title>
            </ListItem.Content>
          }
          isExpanded={showCurrent}
          onPress={() => setShowCurrent(!showCurrent)}>
          {currentEvents.map((event, idx) => (
            <EventListItem
              key={event.id}
              eventId={event.id}
              eventType={event.eventType}
              isFirstItem={idx === 0}
              isLastItem={idx === currentEvents.length - 1}
              testID={`current_event_selector_${idx}`}
            />
          ))}
        </ListItem.Accordion>
        <ListItem.Accordion
          containerStyle={List.accordionItem}
          style={List.accordionItem}
          content={
            <ListItem.Content>
              <ListItem.Title style={[Typography.base, Typography.important]}>
                {STRINGS.events_list_past}
              </ListItem.Title>
            </ListItem.Content>
          }
          isExpanded={showPast}
          onPress={() => setShowPast(!showPast)}>
          {pastEvents.map((event, idx) => (
            <EventListItem
              key={event.id}
              eventId={event.id}
              eventType={event.eventType}
              isFirstItem={idx === 0}
              isLastItem={idx === pastEvents.length - 1}
              testID={`previous_event_selector_${idx}`}
            />
          ))}
        </ListItem.Accordion>
      </View>
    </View>
  );
};

export default EventList;
