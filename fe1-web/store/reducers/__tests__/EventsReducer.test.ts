import 'jest-extended';
import { AnyAction } from 'redux';
import {
  Hash, Meeting, RollCall, RollCallStatus, Timestamp,
} from 'model/objects';
import {
  addEvent, clearAllEvents, eventReduce, removeEvent, updateEvent,
} from '../EventsReducer';
import { mockLaoId } from './SocialReducer.test';

const emptyLao = {
  byLaoId: {},
};

const emptyState = {
  byLaoId: {
    myLaoId: {
      byId: {},
      allIds: [],
      idAlias: {},
    },
  },
};

const mockTime1 = new Timestamp(160000000);
const mockTime2 = new Timestamp(160050000);

const rollCallId : Hash = new Hash('1234');
const rollCallCreated = new RollCall({
  id: rollCallId,
  name: 'roll call',
  location: 'BC',
  creation: mockTime1,
  proposed_start: mockTime1,
  proposed_end: mockTime1,
  status: RollCallStatus.CREATED,
}).toState();

const filledStateWithRollCallCreated = {
  byLaoId: {
    myLaoId: {
      byId: {},
      allIds: [],
      idAlias: {},
    },
    [mockLaoId]: {
      byId: { [rollCallCreated.id]: rollCallCreated },
      allIds: [rollCallCreated.id],
      idAlias: {},
    },
  },
};

const rollCallOpened = new RollCall({
  id: rollCallId,
  name: 'roll call',
  location: 'BC',
  creation: mockTime1,
  proposed_start: mockTime1,
  proposed_end: mockTime1,
  idAlias: new Hash('5678'),
  opened_at: mockTime1,
  status: RollCallStatus.OPENED,
}).toState();

const filledStateWithRollCallOpened = {
  byLaoId: {
    myLaoId: {
      byId: {},
      allIds: [],
      idAlias: {},
    },
    [mockLaoId]: {
      byId: { [rollCallId.toString()]: rollCallOpened },
      allIds: [rollCallId.toString()],
      idAlias: {
        [rollCallOpened.idAlias!]: rollCallOpened.id,
      },
    },
  },
};

const meeting = new Meeting({
  id: new Hash('12345'),
  name: 'meeting',
  location: 'BC',
  creation: mockTime2,
  start: mockTime2,
  end: undefined,
  extra: {},
}).toState();

const filledStateAfterAddedMeeting = {
  byLaoId: {
    myLaoId: {
      byId: {},
      allIds: [],
      idAlias: {},
    },
    [mockLaoId]: {
      byId: {
        [meeting.id]: meeting,
        [rollCallId.toString()]: rollCallOpened,
      },
      allIds: [meeting.id, rollCallId.toString()],
      idAlias: {
        [rollCallOpened.idAlias!]: rollCallOpened.id,
      },
    },
  },
};

describe('EventsReducer', () => {
  it('should return the initial state', () => {
    expect(eventReduce(undefined, {} as AnyAction))
      .toEqual(emptyState);
  });

  it('should add the created roll call', () => {
    expect(eventReduce(emptyState, addEvent(mockLaoId, rollCallCreated)))
      .toEqual(filledStateWithRollCallCreated);
  });

  it('should update the opened roll call', () => {
    expect(eventReduce(filledStateWithRollCallOpened, updateEvent(mockLaoId, rollCallOpened)))
      .toEqual(filledStateWithRollCallOpened);
  });

  it('should add the meeting correctly', () => {
    expect(eventReduce(filledStateWithRollCallOpened, addEvent(mockLaoId, meeting)))
      .toEqual(filledStateAfterAddedMeeting);
  });

  it('should remove the meeting', () => {
    expect(eventReduce(filledStateAfterAddedMeeting, removeEvent(mockLaoId, meeting.id)))
      .toEqual(filledStateWithRollCallOpened);
  });

  it('should clear all events', () => {
    expect(eventReduce(filledStateAfterAddedMeeting, clearAllEvents()))
      .toEqual(emptyLao);
  });
});
