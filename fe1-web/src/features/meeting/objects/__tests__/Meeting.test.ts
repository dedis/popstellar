import 'jest-extended';

import { Hash, Timestamp } from 'core/objects';

import { Meeting } from '../Meeting';

const ID = new Hash('meetingId');
const TIMESTAMP = new Timestamp(1620355600);
const NAME = 'myMeeting';
const LOCATION = 'location';

describe('Meeting object', () => {
  it('does a state round trip correctly with extra and last_modified', () => {
    const meetingState: any = {
      id: ID.valueOf(),
      start: TIMESTAMP.valueOf(),
      name: NAME,
      location: LOCATION,
      creation: TIMESTAMP.valueOf(),
      lastModified: TIMESTAMP.valueOf(),
      extra: {},
    };
    const meeting = Meeting.fromState(meetingState);
    expect(meeting.toState()).toStrictEqual(meetingState);
  });

  it('does a state round trip correctly without extra and last_modified', () => {
    const meetingState: any = {
      id: ID.valueOf(),
      start: TIMESTAMP.valueOf(),
      name: NAME,
      location: LOCATION,
      creation: TIMESTAMP.valueOf(),
    };
    const expected = {
      id: ID.valueOf(),
      start: TIMESTAMP.valueOf(),
      name: NAME,
      location: LOCATION,
      creation: TIMESTAMP.valueOf(),
      lastModified: TIMESTAMP.valueOf(),
      extra: {},
    };
    const meeting = Meeting.fromState(meetingState);
    expect(meeting.toState()).toStrictEqual(expected);
  });

  it('does a state round trip correctly with end', () => {
    const meetingState: any = {
      id: ID.valueOf(),
      start: TIMESTAMP.valueOf(),
      name: NAME,
      location: LOCATION,
      creation: TIMESTAMP.valueOf(),
      lastModified: TIMESTAMP.valueOf(),
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
      const createWrongMeeting = () =>
        new Meeting({
          start: TIMESTAMP,
          name: NAME,
          location: LOCATION,
          creation: TIMESTAMP,
          lastModified: TIMESTAMP,
          extra: {},
        });
      expect(createWrongMeeting).toThrow(Error);
    });

    it('throws an error when name is undefined', () => {
      const createWrongMeeting = () =>
        new Meeting({
          id: ID,
          start: TIMESTAMP,
          location: LOCATION,
          creation: TIMESTAMP,
          lastModified: TIMESTAMP,
          extra: {},
        });
      expect(createWrongMeeting).toThrow(Error);
    });

    it('throws an error when creation is undefined', () => {
      const createWrongMeeting = () =>
        new Meeting({
          id: ID,
          start: TIMESTAMP,
          name: NAME,
          location: LOCATION,
          lastModified: TIMESTAMP,
          extra: {},
        });
      expect(createWrongMeeting).toThrow(Error);
    });

    it('throws an error when start is undefined', () => {
      const createWrongMeeting = () =>
        new Meeting({
          id: ID,
          name: NAME,
          location: LOCATION,
          creation: TIMESTAMP,
          lastModified: TIMESTAMP,
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
        lastModified: TIMESTAMP,
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
        lastModified: TIMESTAMP,
        extra: {},
      });
      expect(meeting).toStrictEqual(expected);
    });
  });
});
