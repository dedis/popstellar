import { Hash, Timestamp } from 'core/objects';
import { getStore } from 'core/redux';

import { EventState } from '../objects';
import { getEvent } from '../reducer';

/**
 * Retrieves an event from the global redux store
 * @param eventId The id of the event to retrieve
 * @returns The event
 */
export const getEventById = (eventId: Hash | string) => getEvent(eventId, getStore().getState());

export { makeEventByTypeSelector, makeEventSelector } from '../reducer';

const CURRENT_EVENTS_THRESHOLD_MINUTES = 15;

export const categorizeEventsByTime = (time: Timestamp, events: EventState[]) => {
  const t = time.valueOf();

  const pastEvents: EventState[] = [];
  const currentEvents: EventState[] = [];
  const upcomingEvents: EventState[] = [];

  events.forEach((e: EventState) => {
    // if end time is set, the event has ended
    if (e.end) {
      pastEvents.push(e);
      return;
    }

    // if end time was not set yet, it is either a current event
    // or an upcoming one

    // current events are the ones that already started or will start within
    // the next {CURRENT_EVENTS_THRESHOLD_MINUTES} minutes
    if (e.start <= t + CURRENT_EVENTS_THRESHOLD_MINUTES * 60 * 1000) {
      currentEvents.push(e);
      return;
    }

    upcomingEvents.push(e);
  });

  return { pastEvents, currentEvents, upcomingEvents };
};
