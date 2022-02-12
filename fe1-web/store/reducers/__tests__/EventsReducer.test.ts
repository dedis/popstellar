import 'jest-extended';
import { AnyAction } from 'redux';
import {
  Hash, LaoEventType, Meeting, MeetingState, RollCall, RollCallState, RollCallStatus, Timestamp,
} from 'model/objects';
import { mockLaoId } from '__tests__/utils/TestUtils';
import {
  addEvent,
  clearAllEvents,
  eventReduce,
  makeEventByTypeSelector,
  makeEventGetter,
  makeEventsAliasMap,
  makeEventsList,
  makeEventsMap,
  makeLastRollCallAttendeesList,
  removeEvent,
  updateEvent,
} from '../EventsReducer';

let emptyLao: any;
let emptyState: any;
let filledStateWithRollCallCreated: any;
let filledStateWithRollCallOpened: any;
let filledStateAfterAddedMeeting: any;

let rollCallIdString: string;
let idAliasString: string;

let rollCallCreated: RollCallState;
let rollCallOpened: RollCallState;
let meeting: MeetingState;

const initialiseData = () => {
  emptyLao = {
    byLaoId: {},
  };

  emptyState = {
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

  rollCallIdString = '1234';
  const rollCallId = new Hash(rollCallIdString);

  rollCallCreated = new RollCall({
    id: rollCallId,
    name: 'roll call',
    location: 'BC',
    creation: mockTime1,
    proposed_start: mockTime1,
    proposed_end: mockTime1,
    status: RollCallStatus.CREATED,
  }).toState();

  filledStateWithRollCallCreated = {
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

  idAliasString = '5678';
  rollCallOpened = new RollCall({
    id: rollCallId,
    name: 'roll call',
    location: 'BC',
    creation: mockTime1,
    proposed_start: mockTime1,
    proposed_end: mockTime1,
    idAlias: new Hash(idAliasString),
    opened_at: mockTime1,
    status: RollCallStatus.OPENED,
  }).toState();

  filledStateWithRollCallOpened = {
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

  meeting = new Meeting({
    id: new Hash('12345'),
    name: 'meeting',
    location: 'BC',
    creation: mockTime2,
    start: mockTime2,
    end: undefined,
    extra: {},
  }).toState();

  filledStateAfterAddedMeeting = {
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
};

beforeAll(() => {
  initialiseData();
});

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

describe('event selector', () => {
  it('should return an empty list of makeEventsList when no lao is opened', () => {
    expect(makeEventsList().resultFunc(emptyState, undefined))
      .toEqual([]);
  });

  it('should return an empty makeEventsList if the state is empty', () => {
    expect(makeEventsList().resultFunc(emptyState, mockLaoId))
      .toEqual([]);
  });

  it('should return makeEventsList correctly', () => {
    expect(makeEventsList().resultFunc(filledStateWithRollCallCreated, mockLaoId))
      .toEqual([RollCall.fromState(rollCallCreated)]);
  });

  it('should return an empty makeEventsAliasMap when no lao is opened', () => {
    expect(makeEventsAliasMap().resultFunc(emptyState, undefined))
      .toEqual({});
  });

  it('should return makeEventsAliasMap correctly', () => {
    expect(makeEventsAliasMap().resultFunc(filledStateWithRollCallOpened, mockLaoId))
      .toEqual({ [idAliasString]: rollCallIdString });
  });

  it('should return an empty makeEventsMap when no lao is opened', () => {
    expect(makeEventsMap().resultFunc(emptyState, undefined))
      .toEqual({});
  });

  it('should return makeEventsMap correctly', () => {
    expect(makeEventsMap().resultFunc(filledStateWithRollCallCreated, mockLaoId))
      .toEqual({ [rollCallIdString]: RollCall.fromState(rollCallCreated) });
  });

  it('should return undefined for makeEventGetter if the state is empty', () => {
    expect(makeEventGetter(mockLaoId, rollCallIdString).resultFunc(emptyState))
      .toEqual(undefined);
  });

  it('should return makeEventGetter correctly', () => {
    expect(makeEventGetter(mockLaoId, rollCallIdString).resultFunc(filledStateWithRollCallCreated))
      .toEqual(RollCall.fromState(rollCallCreated));
  });

  it('should return makeEventByTypeSelector correctly', () => {
    expect(makeEventByTypeSelector(LaoEventType.ROLL_CALL)
      .resultFunc(filledStateWithRollCallCreated))
      .toEqual({
        [mockLaoId]: { [rollCallIdString]: RollCall.fromState(rollCallCreated) },
        myLaoId: {},
      });
  });

  it('should return an empty list for lastRollCallAttendeesList if the state is empty', () => {
    expect(makeLastRollCallAttendeesList(mockLaoId, '1234').resultFunc(emptyState))
      .toEqual([]);
  });
});
