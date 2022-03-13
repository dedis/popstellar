import { Hash } from 'core/objects';
import { getStore } from 'core/redux';

import { LaoEvent } from '../objects';
import { selectCurrentLaoEventsMap } from '../reducer';

export namespace EventStore {
  // Consider using an alternative way to access the store wherever possible
  export function getEvent(id: Hash): LaoEvent | undefined {
    const events = selectCurrentLaoEventsMap(getStore().getState());

    if (!(id.valueOf() in events)) {
      return undefined;
    }

    return events[id.valueOf()];
  }
}
