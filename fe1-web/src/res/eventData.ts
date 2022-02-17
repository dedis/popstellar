// fake Event to show how the app works
import { Hash, LaoEventState, LaoEventType, MeetingState, RollCallState } from 'model/objects';

const LOCATION_TEST = 'A location, test.com';
const LOCATION_EPFL = 'EPFL INF';
const TIMESTAMP_1 = 1607616483; // 10 December 2020 16:08:03 GMT
const TIMESTAMP_2 = 1607618490; // 10 December 2020 16:41:30 GMT

const eventData: LaoEventState[] = [
  {
    id: Hash.fromString('1').toString(),
    eventType: LaoEventType.MEETING,
    name: 'Event 1',
    creation: TIMESTAMP_1,
    last_modified: TIMESTAMP_1,
    location: LOCATION_TEST,
    start: 1607616500,
    end: TIMESTAMP_2,
  } as MeetingState,
  {
    id: Hash.fromString('2').toString(),
    name: 'Event 2',
    eventType: LaoEventType.MEETING,
    creation: TIMESTAMP_1,
    last_modified: TIMESTAMP_1,
    location: LOCATION_TEST,
    start: TIMESTAMP_1,
    end: TIMESTAMP_2,
  } as MeetingState,
  {
    id: Hash.fromString('3').toString(),
    name: 'Event 3',
    eventType: LaoEventType.MEETING,
    creation: TIMESTAMP_1,
    last_modified: TIMESTAMP_1,
    location: LOCATION_TEST,
    start: TIMESTAMP_1,
    end: TIMESTAMP_2,
  } as MeetingState,
  {
    id: Hash.fromString('4').toString(),
    name: 'Event 4',
    eventType: LaoEventType.ROLL_CALL,
    creation: TIMESTAMP_1,
    last_modified: TIMESTAMP_1,
    start: TIMESTAMP_1,
    proposed_start: TIMESTAMP_1,
    proposed_end: TIMESTAMP_2,
    location: LOCATION_EPFL,
    status: 0,
  } as RollCallState,
  {
    id: Hash.fromString('5').toString(),
    name: 'Event 5',
    eventType: LaoEventType.ROLL_CALL,
    creation: TIMESTAMP_1,
    last_modified: TIMESTAMP_1,
    start: TIMESTAMP_1,
    proposed_start: TIMESTAMP_1,
    opened_at: 1607617083,
    proposed_end: TIMESTAMP_2,
    location: LOCATION_EPFL,
    status: 1,
  } as RollCallState,
  {
    id: Hash.fromString('6').toString(),
    name: 'Event 6',
    eventType: LaoEventType.MEETING,
    creation: TIMESTAMP_1,
    last_modified: TIMESTAMP_1,
    start: TIMESTAMP_1,
    end: TIMESTAMP_2,
  } as MeetingState,
  {
    id: Hash.fromString('7').toString(),
    name: 'Event 7',
    eventType: LaoEventType.MEETING,
    creation: TIMESTAMP_1,
    last_modified: TIMESTAMP_1,
    location: LOCATION_TEST,
    start: TIMESTAMP_1,
    end: TIMESTAMP_2,
  } as MeetingState,
  {
    id: Hash.fromString('8').toString(),
    name: 'Event 8',
    eventType: LaoEventType.MEETING,
    creation: TIMESTAMP_1,
    last_modified: TIMESTAMP_1,
    location: LOCATION_TEST,
    start: TIMESTAMP_1,
    end: TIMESTAMP_2,
  } as MeetingState,
  {
    id: Hash.fromString('9').toString(),
    name: 'Event 9',
    eventType: LaoEventType.MEETING,
    creation: TIMESTAMP_1,
    last_modified: TIMESTAMP_1,
    location: LOCATION_TEST,
    start: TIMESTAMP_1,
    end: TIMESTAMP_2,
  } as MeetingState,
  {
    id: Hash.fromString('10').toString(),
    name: 'Event 10',
    eventType: LaoEventType.MEETING,
    creation: TIMESTAMP_1,
    last_modified: TIMESTAMP_1,
    location: LOCATION_TEST,
    start: TIMESTAMP_1,
    end: TIMESTAMP_2,
  } as MeetingState,
  {
    id: Hash.fromString('11').toString(),
    name: 'Event 11',
    eventType: LaoEventType.MEETING,
    start: 607616483,
    creation: TIMESTAMP_1,
    last_modified: TIMESTAMP_1,
    end: 1607616490,
    location: LOCATION_EPFL,
  } as MeetingState,
  {
    id: Hash.fromString('12').toString(),
    name: 'Event 12',
    eventType: LaoEventType.MEETING,
    creation: TIMESTAMP_1,
    last_modified: TIMESTAMP_1,
    location: LOCATION_TEST,
    start: TIMESTAMP_1,
    end: TIMESTAMP_2,
  } as MeetingState,
  {
    id: Hash.fromString('13').toString(),
    name: 'Event 13',
    eventType: LaoEventType.MEETING,
    creation: TIMESTAMP_1,
    last_modified: TIMESTAMP_1,
    location: LOCATION_TEST,
    start: TIMESTAMP_1,
    end: TIMESTAMP_2,
  } as MeetingState,
  {
    id: Hash.fromString('14').toString(),
    name: 'Event 14',
    eventType: LaoEventType.MEETING,
    creation: TIMESTAMP_1,
    last_modified: TIMESTAMP_1,
    location: LOCATION_TEST,
    start: TIMESTAMP_1,
    end: TIMESTAMP_2,
  } as MeetingState,
  {
    id: Hash.fromString('15').toString(),
    name: 'Event 15',
    eventType: LaoEventType.MEETING,
    creation: TIMESTAMP_1,
    last_modified: TIMESTAMP_1,
    location: LOCATION_TEST,
    start: TIMESTAMP_1,
    end: TIMESTAMP_2,
  } as MeetingState,
  {
    id: Hash.fromString('16').toString(),
    name: 'Event 16',
    eventType: LaoEventType.MEETING,
    creation: TIMESTAMP_1,
    last_modified: TIMESTAMP_1,
    location: LOCATION_TEST,
    start: TIMESTAMP_1,
    end: TIMESTAMP_2,
  } as MeetingState,
  {
    id: Hash.fromString('17').toString(),
    name: 'Event 17',
    eventType: LaoEventType.MEETING,
    creation: TIMESTAMP_1,
    last_modified: TIMESTAMP_1,
    location: LOCATION_TEST,
    start: TIMESTAMP_1,
    end: TIMESTAMP_2,
  } as MeetingState,
  {
    id: Hash.fromString('18').toString(),
    name: 'Event 18',
    eventType: LaoEventType.MEETING,
    creation: TIMESTAMP_1,
    last_modified: TIMESTAMP_1,
    location: LOCATION_TEST,
    start: TIMESTAMP_1,
    end: TIMESTAMP_2,
  } as MeetingState,
  {
    id: Hash.fromString('19').toString(),
    name: 'Event 19',
    eventType: LaoEventType.MEETING,
    creation: TIMESTAMP_1,
    last_modified: TIMESTAMP_1,
    location: LOCATION_TEST,
    start: TIMESTAMP_1,
    end: TIMESTAMP_2,
  } as MeetingState,
  {
    id: Hash.fromString('20').toString(),
    name: 'Event 20',
    eventType: LaoEventType.MEETING,
    creation: TIMESTAMP_1,
    last_modified: TIMESTAMP_1,
    location: LOCATION_TEST,
    start: TIMESTAMP_1,
    end: TIMESTAMP_2,
  } as MeetingState,
];

const sortByEndTime = (a: LaoEventState, b: LaoEventState) => {
  if (!a.end) {
    if (!b.end) {
      return 0;
    }
    return 1;
  }

  // a.end is defined at this point
  if (!b.end || a.end < b.end) {
    return -1;
  }

  // b.end is defined at this point
  if (a.end === b.end) {
    return 0;
  }

  return 1;
};

function sortByStartAscending(a: LaoEventState, b: LaoEventState) {
  if (!a || !a.start) {
    return 1;
  }
  if (!b || !b.start || a.start < b.start) {
    return -1;
  }

  if (a.start === b.start) {
    sortByEndTime(a, b);
  }

  return 1;
}

// sort in place
eventData.sort(sortByStartAscending);

export default eventData;
