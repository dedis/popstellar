import { Hash } from 'core/objects';
import { getStore } from 'core/redux';

import { getEvent } from '../reducer';

/**
 * Retrieves an event from the global redux store
 * @param eventId The id of the event to retrieve
 * @returns The event
 */
export const getEventById = (eventId: Hash | string) => getEvent(eventId, getStore().getState());

export { makeEventByTypeSelector, makeEventSelector } from '../reducer';
