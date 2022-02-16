import { Hash } from 'model/objects';

import { LaoEvent } from '../objects';
import { makeEventsAliasMap, makeEventsMap } from '../reducer/EventsReducer';

const getEventMap = makeEventsMap();
const getEventAliases = makeEventsAliasMap();

/**
 * Retrieves the event id associated with a given alias.
 *
 * @param state - The store state
 * @param id - The id (or alias) to be found
 *
 * @returns LaoEvent associated with the id, if found
 * @returns undefined if the id doesn't match any known event ID or alias
 */
export function getEventFromId(state: any, id: Hash): LaoEvent | undefined {
  const eventAlias = getEventAliases(state);
  const eventMap = getEventMap(state);

  const idStr = id.valueOf();
  const evtId = (idStr in eventAlias)
    ? eventAlias[idStr]
    : idStr;

  return (evtId in eventMap)
    ? eventMap[evtId]
    : undefined;
}
