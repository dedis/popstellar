import React, { useMemo, useState } from 'react';
import { View } from 'react-native';
import { ListItem } from 'react-native-elements';
import { useSelector } from 'react-redux';

import { Timestamp } from 'core/objects';
import { List, Typography } from 'core/styles';
import STRINGS from 'resources/strings';

import { EventHooks } from '../hooks';
import { EventState } from '../objects';
import { makeEventListSelector } from '../reducer';
import EventListItem from './EventListItem';

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
 */
const EventList = () => {
  const laoId = EventHooks.useCurrentLaoId();

  if (!laoId) {
    throw new Error('Cannot show an event list if you are not connected to a lao!');
  }

  const [showUpcoming, setShowUpcoming] = useState(true);
  const [showCurrent, setShowCurrent] = useState(true);
  const [showPast, setShowPast] = useState(false);

  const eventListSelector = useMemo(() => makeEventListSelector(laoId.valueOf()), [laoId]);
  const events = useSelector(eventListSelector);
  const [pastEvents, currentEvents, futureEvents] = categorizeEventsByTime(
    Timestamp.EpochNow().valueOf(),
    events,
  );

  return (
    <View style={List.container}>
      <ListItem.Accordion
        containerStyle={List.accordionItem}
        style={List.accordionItem}
        content={
          <ListItem.Content>
            <ListItem.Title style={[Typography.base, Typography.important]}>
              {STRINGS.events_list_upcoming} ({futureEvents.length})
            </ListItem.Title>
          </ListItem.Content>
        }
        isExpanded={showUpcoming}
        onPress={() => setShowUpcoming(!showUpcoming)}>
        {futureEvents.map((event, idx) => (
          <EventListItem
            key={event.id}
            eventId={event.id}
            eventType={event.eventType}
            isFirstItem={idx === 0}
            isLastItem={idx === futureEvents.length - 1}
            testID={`upcoming_event_selector_${idx}`}
          />
        ))}
      </ListItem.Accordion>
      <ListItem.Accordion
        containerStyle={List.accordionItem}
        style={List.accordionItem}
        content={
          <ListItem.Content>
            <ListItem.Title style={[Typography.base, Typography.important]}>
              {STRINGS.events_list_current} ({currentEvents.length})
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
              {STRINGS.events_list_past} ({pastEvents.length})
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
  );
};

export default EventList;
