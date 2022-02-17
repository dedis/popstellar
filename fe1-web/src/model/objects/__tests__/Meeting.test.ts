import 'jest-extended';
import { Meeting, MeetingState } from '../Meeting';
import { LaoEventType } from '../LaoEvent';
import { Hash } from '../Hash';
import { Timestamp } from '../Timestamp';

const ID = new Hash('meetingId');
const TIMESTAMP = new Timestamp(1620255600);
const NAME = 'myMeeting';
const LOCATION = 'location';

describe('Meeting object', () => {
  it('does a state round trip correctly with extra and last_modified', () => {
    const meetingState: MeetingState = {
      id: ID.valueOf(),
      eventType: LaoEventType.MEETING,
      start: TIMESTAMP.valueOf(),
      name: NAME,
      location: LOCATION,
      creation: TIMESTAMP.valueOf(),
      last_modified: TIMESTAMP.valueOf(),
      extra: {},
    };
    const meeting = Meeting.fromState(meetingState);
    expect(meeting.toState()).toStrictEqual(meetingState);
  });

  it('does a state round trip correctly without extra and last_modified', () => {
    const meetingState = {
      id: ID.valueOf(),
      eventType: LaoEventType.MEETING,
      start: TIMESTAMP.valueOf(),
      name: NAME,
      location: LOCATION,
      creation: TIMESTAMP.valueOf(),
    };
    const expected: MeetingState = {
      id: ID.valueOf(),
      eventType: LaoEventType.MEETING,
      start: TIMESTAMP.valueOf(),
      name: NAME,
      location: LOCATION,
      creation: TIMESTAMP.valueOf(),
      last_modified: TIMESTAMP.valueOf(),
      extra: {},
    };
    const meeting = Meeting.fromState(meetingState as MeetingState);
    expect(meeting.toState()).toStrictEqual(expected);
  });

  it('does a state round trip correctly with end', () => {
    const meetingState: MeetingState = {
      id: ID.valueOf(),
      eventType: LaoEventType.MEETING,
      start: TIMESTAMP.valueOf(),
      name: NAME,
      location: LOCATION,
      creation: TIMESTAMP.valueOf(),
      last_modified: TIMESTAMP.valueOf(),
      extra: {},
      end: TIMESTAMP.valueOf() + 500,
    };
    const meeting = Meeting.fromState(meetingState);
    expect(meeting.toState()).toStrictEqual(meetingState);
  });

  describe('constructor', () => {
    it('throws an error when object is undefined', () => {
      const partial = undefined as unknown as Partial<Meeting>;
      const createWrongMeeting = () => new Meeting(partial);
      expect(createWrongMeeting).toThrow(Error);
    });

    it('throws an error when object is null', () => {
      const partial = null as unknown as Partial<Meeting>;
      const createWrongMeeting = () => new Meeting(partial);
      expect(createWrongMeeting).toThrow(Error);
    });

    it('throws an error when id is undefined', () => {
      const createWrongMeeting = () => new Meeting({
        start: TIMESTAMP,
        name: NAME,
        location: LOCATION,
        creation: TIMESTAMP,
        last_modified: TIMESTAMP,
        extra: {},
      });
      expect(createWrongMeeting).toThrow(Error);
    });

    it('throws an error when name is undefined', () => {
      const createWrongMeeting = () => new Meeting({
        id: ID,
        start: TIMESTAMP,
        location: LOCATION,
        creation: TIMESTAMP,
        last_modified: TIMESTAMP,
        extra: {},
      });
      expect(createWrongMeeting).toThrow(Error);
    });

    it('throws an error when creation is undefined', () => {
      const createWrongMeeting = () => new Meeting({
        id: ID,
        start: TIMESTAMP,
        name: NAME,
        location: LOCATION,
        last_modified: TIMESTAMP,
        extra: {},
      });
      expect(createWrongMeeting).toThrow(Error);
    });

    it('throws an error when start is undefined', () => {
      const createWrongMeeting = () => new Meeting({
        id: ID,
        name: NAME,
        location: LOCATION,
        creation: TIMESTAMP,
        last_modified: TIMESTAMP,
        extra: {},
      });
      expect(createWrongMeeting).toThrow(Error);
    });

    it('creates a correct meeting when last_modified is undefined', () => {
      const meeting = new Meeting({
        id: ID,
        start: TIMESTAMP,
        name: NAME,
        location: LOCATION,
        creation: TIMESTAMP,
        extra: {},
      });

      const expected = new Meeting({
        id: ID,
        start: TIMESTAMP,
        name: NAME,
        location: LOCATION,
        creation: TIMESTAMP,
        last_modified: TIMESTAMP,
        extra: {},
      });
      expect(meeting).toStrictEqual(expected);
    });

    it('creates a correct meeting when extra is undefined', () => {
      const meeting = new Meeting({
        id: ID,
        start: TIMESTAMP,
        name: NAME,
        location: LOCATION,
        creation: TIMESTAMP,
      });

      const expected = new Meeting({
        id: ID,
        start: TIMESTAMP,
        name: NAME,
        location: LOCATION,
        creation: TIMESTAMP,
        last_modified: TIMESTAMP,
        extra: {},
      });
      expect(meeting).toStrictEqual(expected);
    });
  });
});
