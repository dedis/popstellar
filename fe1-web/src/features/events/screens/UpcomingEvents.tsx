import { ListItem } from '@rneui/themed';
import React, { useEffect, useMemo, useState } from 'react';
import { View } from 'react-native';
import { useSelector } from 'react-redux';

import ScreenWrapper from 'core/components/ScreenWrapper';
import { Timestamp } from 'core/objects';
import { List, Typography } from 'core/styles';
import STRINGS from 'resources/strings';

import { EventListItem } from '../components';
import { categorizeEventsByTime } from '../functions';
import { EventHooks } from '../hooks';
import { EventFeature } from '../interface/Feature';
import { EventState } from '../objects';
import { makeEventListSelector } from '../reducer';

const UpcomingEvents = () => {
  const laoId = EventHooks.useAssertCurrentLaoId();
  const eventListSelector = useMemo(() => makeEventListSelector(laoId.valueOf()), [laoId]);
  const events = useSelector(eventListSelector);

  const [upcomingEvents, setEvents] = useState<EventState[]>(
    () => categorizeEventsByTime(Timestamp.EpochNow(), events).upcomingEvents,
  );

  useEffect(() => {
    const interval = setInterval(
      () => setEvents(categorizeEventsByTime(Timestamp.EpochNow(), events).upcomingEvents),
      1000,
    );

    // clear the interval when unmouting the component
    return () => clearInterval(interval);
  }, [events]);

  const [showUpcoming, setShowUpcoming] = useState(true);

  return (
    <ScreenWrapper>
      <View style={List.container}>
        <ListItem.Accordion
          containerStyle={List.accordionItem}
          style={List.accordionItem}
          content={
            <ListItem.Content>
              <ListItem.Title style={[Typography.base, Typography.important]}>
                {STRINGS.events_list_upcoming}
              </ListItem.Title>
            </ListItem.Content>
          }
          isExpanded={showUpcoming}
          onPress={() => setShowUpcoming(!showUpcoming)}>
          {upcomingEvents.map((event, idx) => (
            <EventListItem
              key={event.id}
              eventId={event.id}
              eventType={event.eventType}
              isFirstItem={idx === 0}
              isLastItem={idx === upcomingEvents.length - 1}
              testID={`upcoming_event_selector_${idx}`}
            />
          ))}
        </ListItem.Accordion>
      </View>
    </ScreenWrapper>
  );
};

export default UpcomingEvents;

export const UpcomingEventsScreen: EventFeature.LaoEventScreen = {
  id: STRINGS.navigation_lao_events_upcoming,
  Component: UpcomingEvents,
};
