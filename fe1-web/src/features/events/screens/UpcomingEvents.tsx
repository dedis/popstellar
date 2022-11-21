import React, { useEffect, useMemo, useState } from 'react';
import { useSelector } from 'react-redux';

import ScreenWrapper from 'core/components/ScreenWrapper';
import { Timestamp } from 'core/objects';
import STRINGS from 'resources/strings';

import EventList from '../components/EventList';
import { categorizeEventsByTime } from '../functions';
import { EventHooks } from '../hooks';
import { EventFeature } from '../interface/Feature';
import { EventState } from '../objects';
import { makeEventListSelector } from '../reducer';

const UpcomingEvents = () => {
  const laoId = EventHooks.useCurrentLaoId();
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

  return (
    <ScreenWrapper>
      <EventList title={STRINGS.events_list_upcoming} toggleable={false} events={upcomingEvents} />
    </ScreenWrapper>
  );
};

export default UpcomingEvents;

export const UpcomingEventsScreen: EventFeature.LaoEventScreen = {
  id: STRINGS.navigation_lao_events_upcoming,
  Component: UpcomingEvents,
};
