import currentEventsReducer from 'store/reducers/EventsReducer';
import eventsData from 'res/EventData';

const emptyState = {
  events: [
    {
      title: 'Past',
      data: [],
    },
    {
      title: 'Present',
      data: [],
    },
    {
      title: 'Future',
      data: [],
    },
  ],
};

function randomInt(x: number): number {
  return Math.floor(Math.random() * x);
}

function randomTimestamp(x: number): number {
  return Math.floor(Date.now() / 1000) + (randomInt(x) - x / 2) * 100;
}

function createEvent() {
  const objects = ['meeting', 'roll-call'/* , 'poll', 'discussion' */];
  // const actions = ['create', 'update_properties', 'state'];
  const object = objects[randomInt(objects.length)];
  // const action = actions[randomInt(actions.length)];
  const id = randomInt(50000).toString();
  // const name = `Event ${id}`;
  const creation = randomTimestamp(50);
  // const last_modified = creation;
  // const location = randomInt(2) === 1 ? '' : 'A location, test.com';
  const start = creation;
  const end = randomInt(2) === 1 ? undefined : creation + randomInt(50) * 100;
  const organizer = 'a organizer signature';
  const witnesses = ['witness signature 1', 'witness signature 2'];
  // const modification_id = 'a modification id';
  // const modification_signatures = [{ witness: 'Witness 1', signature: 'witness signature 1' },
  // { witness: 'Witness 2', signature: 'witness signature 2' }];

  return {
    id,
    object,
    // action,
    // name,
    // creation,
    // last_modified,
    // location,
    start,
    end,
    organizer,
    witnesses,
    // modification_id,
    // modification_signatures,
  };
}

describe('Current events reducer', () => {
  it('should handle CLEAR_EVENTS', () => {
    expect(currentEventsReducer({ events: eventsData }, {
      type: 'CLEAR_EVENTS',
      value: undefined,
    })).toEqual(emptyState);
  });

  it('should handle add and remove an event', () => {
    const event = createEvent();
    const state1 = currentEventsReducer(emptyState, {
      type: 'ADD_EVENT',
      value: event,
    });
    expect(state1).not.toEqual(emptyState);
    expect(currentEventsReducer(state1, {
      type: 'REMOVE_EVENT',
      value: event.id,
    })).toEqual(emptyState);

    const events = [];
    let state = emptyState;
    for (let i = 0; i < 250; i += 1) {
      events.push(createEvent());
      state = currentEventsReducer(state, {
        type: 'UPDATE_EVENT',
        value: events[i],
      });
    }
    expect(state).not.toEqual(emptyState);
    for (let i = 0; i < events.length; i += 1) {
      state = currentEventsReducer(state, {
        type: 'REMOVE_EVENT',
        value: events[i].id,
      });
    }
    expect(state).toEqual(emptyState);
  });
});
