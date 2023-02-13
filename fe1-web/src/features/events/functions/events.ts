import { Hash, Timestamp } from 'core/objects';
import { getStore } from 'core/redux';

import { EventState } from '../objects';
import { getEvent } from '../reducer';

/**
 * Retrieves an event from the global redux store
 * @param eventId The id of the event to retrieve
 * @returns The event
 */
export const getEventById = (eventId: Hash) => getEvent(eventId, getStore().getState());

export { makeEventByTypeSelector, makeEventSelector } from '../reducer';

const CURRENT_EVENTS_THRESHOLD_HOURS = 24;

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
    // the next {CURRENT_EVENTS_THRESHOLD_HOURS} hours
    if (e.start <= t + CURRENT_EVENTS_THRESHOLD_HOURS * 60 * 60) {
      currentEvents.push(e);
      return;
    }

    upcomingEvents.push(e);
  });

  // current and past events from newest to oldest
  pastEvents.sort((a, b) => b.start - a.start);
  currentEvents.sort((a, b) => b.start - a.start);

  // upcoming events from oldest i.e. closest in the future to newest / furthest
  upcomingEvents.sort((a, b) => b.start - a.start);

  return { pastEvents, currentEvents, upcomingEvents };
};
