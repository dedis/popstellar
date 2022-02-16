import { Hash } from 'model/objects';
import { getStore } from 'store/Storage';

import { LaoEvent } from '../objects';
import { makeEventsMap } from '../reducer/EventsReducer';

export namespace EventStore {

  // Consider using an alternative way to access the store wherever possible
  export function getEvent(id: Hash): LaoEvent | undefined {
    const eventsMap = makeEventsMap();
    const events = eventsMap(getStore().getState());

    if (!(id.valueOf() in events)) {
      return undefined;
    }

    return events[id.valueOf()];
  }
}
