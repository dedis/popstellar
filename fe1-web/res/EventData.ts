// fake Event to show how the app works
import { Hash } from 'model/objects';

// @ts-ignore
const data: LaoEventtate[] = [
  {
    id: Hash.fromString('1').toString(),
    eventType: 'MEETING',
    name: 'Event 1',
    creation: 1607616483,
    last_modified: 1607616483,
    location: 'A location, test.com',
    start: 1607616500,
    end: 1607618490,
  },
  {
    id: Hash.fromString('2').toString(),
    eventType: 'MEETING',
    name: 'Event 2',
    creation: 1607616483,
    last_modified: 1607616483,
    location: 'A location, test.com',
    start: 1607616483,
    end: 1607618490,
  },
  {
    id: Hash.fromString('3').toString(),
    eventType: 'MEETING',
    name: 'Event 3',
    creation: 1607616483,
    last_modified: 1607616483,
    location: 'A location, test.com',
    start: 1607616483,
    end: 1607618490,
  },
  {
    id: Hash.fromString('4').toString(),
    eventType: 'ROLL_CALL',
    name: 'Event 4',
    creation: 1607616483,
    last_modified: 1607616483,
    start: 1607616483,
    end: 1607618490,
    location: 'EPFL INF',
  },
  {
    id: Hash.fromString('5').toString(),
    eventType: 'ROLL_CALL',
    name: 'Event 5',
    creation: 1607616483,
    last_modified: 1607616483,
    start: 1607616483,
    end: 1607618490,
    location: 'EPFL INF',
  },
  {
    id: Hash.fromString('6').toString(),
    eventType: 'MEETING',
    name: 'Event 6',
    creation: 1607616483,
    last_modified: 1607616483,
    start: 1607616483,
    end: 1607618490,
  },
  {
    id: Hash.fromString('7').toString(),
    eventType: 'MEETING',
    name: 'Event 7',
    creation: 1607616483,
    last_modified: 1607616483,
    location: 'A location, test.com',
    start: 1607616483,
    end: 1607618490,
  },
  {
    id: Hash.fromString('8').toString(),
    eventType: 'MEETING',
    name: 'Event 8',
    creation: 1607616483,
    last_modified: 1607616483,
    location: 'A location, test.com',
    start: 1607616483,
    end: 1607618490,
  },
  {
    id: Hash.fromString('9').toString(),
    eventType: 'MEETING',
    name: 'Event 9',
    creation: 1607616483,
    last_modified: 1607616483,
    location: 'A location, test.com',
    start: 1607616483,
    end: 1607618490,
  },
  {
    id: Hash.fromString('10').toString(),
    eventType: 'MEETING',
    name: 'Event 10',
    creation: 1607616483,
    last_modified: 1607616483,
    location: 'A location, test.com',
    start: 1607616483,
    end: 1607618490,
  },
  {
    id: Hash.fromString('11').toString(),
    eventType: 'ROLL_CALL',
    name: 'Event 11',
    start: 1607616483,
    creation: 1607616483,
    last_modified: 1607616483,
    end: 1607616490,
    location: 'EPFL INF',
  },
  {
    id: Hash.fromString('12').toString(),
    eventType: 'MEETING',
    name: 'Event 12',
    creation: 1607616483,
    last_modified: 1607616483,
    location: 'A location, test.com',
    start: 1607616483,
    end: 1607618490,
  },
  {
    id: Hash.fromString('13').toString(),
    eventType: 'MEETING',
    name: 'Event 13',
    creation: 1607616483,
    last_modified: 1607616483,
    location: 'A location, test.com',
    start: 1607616483,
    end: 1607618490,
  },
  {
    id: Hash.fromString('14').toString(),
    eventType: 'MEETING',
    name: 'Event 14',
    creation: 1607616483,
    last_modified: 1607616483,
    location: 'A location, test.com',
    start: 1607616483,
    end: 1607618490,
  },
  {
    id: Hash.fromString('15').toString(),
    eventType: 'MEETING',
    name: 'Event 15',
    creation: 1607616483,
    last_modified: 1607616483,
    location: 'A location, test.com',
    start: 1607616483,
    end: 1607618490,
  },
  {
    id: Hash.fromString('16').toString(),
    eventType: 'MEETING',
    name: 'Event 16',
    creation: 1607616483,
    last_modified: 1607616483,
    location: 'A location, test.com',
    start: 1607616483,
    end: 1607618490,
  },
  {
    id: Hash.fromString('17').toString(),
    eventType: 'MEETING',
    name: 'Event 17',
    creation: 1607616483,
    last_modified: 1607616483,
    location: 'A location, test.com',
    start: 1607616483,
    end: 1607618490,
  },
  {
    id: Hash.fromString('18').toString(),
    eventType: 'MEETING',
    name: 'Event 18',
    creation: 1607616483,
    last_modified: 1607616483,
    location: 'A location, test.com',
    start: 1607616483,
    end: 1607618490,
  },
  {
    id: Hash.fromString('19').toString(),
    eventType: 'MEETING',
    name: 'Event 19',
    creation: 1607616483,
    last_modified: 1607616483,
    location: 'A location, test.com',
    start: 1607616483,
    end: 1607618490,
  },
  {
    id: Hash.fromString('20').toString(),
    eventType: 'MEETING',
    name: 'Event 20',
    creation: 1607616483,
    last_modified: 1607616483,
    location: 'A location, test.com',
    start: 1607616483,
    end: 1607618490,
  },
];

function sortByStartAscending(a: LaoEventtate, b: LaoEventtate) {
  if (a.start < b.start) {
    return -1;
  }

  if (a.start === b.start) {
    if (a.end < b.end) {
      return -1;
    }
    if (a.end === b.end) {
      return 0;
    }
  }

  return 1;
}

// sort in place
data.sort(sortByStartAscending);

export default data;
