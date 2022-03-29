import { Hash } from 'core/objects';

import { LaoEvent } from '../objects';
import { selectEventsAliasMap, selectCurrentLaoEventsMap } from '../reducer';

const getEventAliases = selectEventsAliasMap;

/**
 * Retrieves the event id associated with a given alias.
 *
 * @param state - The store state
 * @param id - The id (or alias) to be found
 *
 * @returns LaoEvent associated with the id, if found
 * @returns undefined if the id doesn't match any known event ID or alias
 */
export function selectEventById(state: any, id: Hash): LaoEvent | undefined {
  const eventAlias = getEventAliases(state);
  const eventMap = selectCurrentLaoEventsMap(state);

  const idStr = id.valueOf();
  const evtId = idStr in eventAlias ? eventAlias[idStr] : idStr;

  return evtId in eventMap ? eventMap[evtId] : undefined;
}
