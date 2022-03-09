import { useRoute } from '@react-navigation/core';
import { Timestamp } from 'core/objects';
import EventListCollapsible from 'features/events/components/EventListCollapsible';
import { LaoEvent } from 'features/events/objects';
import { makeEventsList } from 'features/events/reducer';
import React, { useState } from 'react';
import { ScrollView } from 'react-native';
import { useSelector } from 'react-redux';

import { LaoProperties } from '../components';

const eventList = makeEventsList();

/**
 * AttendeeScreen: lists LAO properties and past/ongoing/future events.
 * By default, only the past and present section are open.
 *
 * TODO: use the data receive by the organization server
 */
const AttendeeScreen = () => {
  const events = useSelector(eventList);
  const now = Timestamp.EpochNow();
  const pastEvents: LaoEvent[] = [];
  const currentEvents: LaoEvent[] = [];
  const futureEvents: LaoEvent[] = [];
  // FIXME: route should use proper type
  const route = useRoute<any>();
  const { url } = route.params || '';
  const [serverUrl] = useState(url);

  events.forEach((e: LaoEvent) => {
    if ((e.end && e.end.before(now)) || (!e.end && e.start.before(now))) {
      pastEvents.push(e);
      return;
    }
    if (e.start.after(now)) {
      futureEvents.push(e);
      return;
    }
    currentEvents.push(e);
  });
  // TODO: nesting logic

  const DATA_EXAMPLE = [
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

  return (
    <ScrollView>
      <LaoProperties url={serverUrl} />
      <EventListCollapsible data={DATA_EXAMPLE} />
    </ScrollView>
  );
};

export default AttendeeScreen;
