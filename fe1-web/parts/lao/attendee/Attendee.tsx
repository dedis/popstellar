import React from 'react';
import { useSelector } from 'react-redux';

import { makeEventsList } from 'store';
import { Event, Timestamp } from 'model/objects';

import EventListCollapsible from 'components/eventList/EventListCollapsible';
import LaoProperties from 'components/eventList/LaoProperties';

const laoToProperties = (events: any) => [[], ...events];

/**
 * Attendee screen: lists LAO properties and past/ongoing/future events
 *
 * TODO By default only the past and present section are open.
 * TODO use the data receive by the organization server
*/
const Attendee = () => {
  const eventList = makeEventsList();
  const events = useSelector(eventList);

  const now = Timestamp.EpochNow();
  const pastEvents: Event[] = [];
  const currentEvents: Event[] = [];
  const futureEvents: Event[] = [];

  events.forEach((e: Event) => {
    if (e.end.before(now)) {
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
    <>
      <LaoProperties />
      <EventListCollapsible data={DATA_EXAMPLE} />
    </>
  );
};

export default Attendee;
