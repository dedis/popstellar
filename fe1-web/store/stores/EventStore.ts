import { LaoEvent, Hash } from 'model/objects';
import { getStore } from '../Storage';
import { makeEventsMap } from '../reducers';

const eventsMap = makeEventsMap();

export namespace EventStore {

  // Consider using an alternative way to access the store wherever possible
  export function getEvent(id: Hash): LaoEvent | undefined {
    const events = eventsMap(getStore().getState());

    if (!(id.valueOf() in events)) {
      return undefined;
    }

    return events[id.valueOf()];
  }
}
