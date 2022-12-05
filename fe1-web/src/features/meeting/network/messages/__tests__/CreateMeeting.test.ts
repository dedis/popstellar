import 'jest-extended';
import '__tests__/utils/matchers';

import { configureTestFeatures, mockLaoId } from '__tests__/utils';
import { ActionType, ObjectType } from 'core/network/jsonrpc/messages';
import { Hash, ProtocolError, Timestamp } from 'core/objects';

import { CreateMeeting } from '../CreateMeeting';

const NAME = 'myMeeting';
const LOCATION = 'location';
const TIMESTAMP = new Timestamp(1609455600); // 1st january 2021
const FUTURE_TIMESTAMP = new Timestamp(1735686000); // 1st january 2025
const mockMeetingId = Hash.fromStringArray('M', mockLaoId, TIMESTAMP.toString(), NAME);
const mockExtra = { extra: 'extra info' };

const sampleCreateMeeting: Partial<CreateMeeting> = {
  object: ObjectType.MEETING,
  action: ActionType.CREATE,
  id: mockMeetingId,
  name: NAME,
  creation: TIMESTAMP,
  location: LOCATION,
  start: TIMESTAMP,
  end: FUTURE_TIMESTAMP,
  extra: mockExtra,
};

const createMeetingJson = `{
  "object": "${ObjectType.MEETING}",
  "action": "${ActionType.CREATE}",
  "id": "${mockMeetingId}",
  "name": "${NAME}",
  "creation": ${TIMESTAMP},
  "location": "${LOCATION}",
  "start": ${TIMESTAMP},
  "end": ${FUTURE_TIMESTAMP},
  "extra": ${JSON.stringify(mockExtra)}
}`;

beforeAll(() => {
  configureTestFeatures();
});

describe('CreateMeeting', () => {
  it('should be created correctly from Json', () => {
    expect(new CreateMeeting(sampleCreateMeeting, mockLaoId)).toBeJsonEqual(sampleCreateMeeting);

    let temp: any = {
      object: ObjectType.MEETING,
      action: ActionType.CREATE,
      id: mockMeetingId,
      name: NAME,
      creation: TIMESTAMP,
      location: LOCATION,
      start: TIMESTAMP,
      end: FUTURE_TIMESTAMP,
      extra: mockExtra,
    };
    expect(new CreateMeeting(temp, mockLaoId)).toBeJsonEqual(temp);

    temp = {
      object: ObjectType.MEETING,
      action: ActionType.CREATE,
      id: mockMeetingId,
      name: NAME,
      creation: TIMESTAMP,
      start: TIMESTAMP,
    };
    expect(new CreateMeeting(temp, mockLaoId)).toBeJsonEqual(temp);
  });

  it('should be parsed correctly from Json', () => {
    const obj = JSON.parse(createMeetingJson);
    expect(CreateMeeting.fromJson(obj, mockLaoId)).toBeJsonEqual(sampleCreateMeeting);
  });

  it('fromJson should throw an error if the Json has incorrect action', () => {
    const obj = {
      object: ObjectType.MEETING,
      action: ActionType.CREATE,
      id: mockMeetingId,
      name: NAME,
      creation: TIMESTAMP,
      location: LOCATION,
      start: TIMESTAMP,
      end: FUTURE_TIMESTAMP,
      extra: mockExtra,
    };
    const createWrongObj = () => CreateMeeting.fromJson(obj, mockLaoId);
    expect(createWrongObj).toThrow(ProtocolError);
  });

  describe('constructor', () => {
    it('should throw an error if name is undefined', () => {
      const createWrongObj = () =>
        new CreateMeeting(
          {
            object: ObjectType.MEETING,
            action: ActionType.CREATE,
            id: mockMeetingId,
            creation: TIMESTAMP,
            location: LOCATION,
            start: TIMESTAMP,
            end: FUTURE_TIMESTAMP,
            extra: mockExtra,
          },
          mockLaoId,
        );
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if create is undefined', () => {
      const createWrongObj = () =>
        new CreateMeeting(
          {
            object: ObjectType.MEETING,
            action: ActionType.CREATE,
            id: mockMeetingId,
            name: NAME,
            location: LOCATION,
            start: TIMESTAMP,
            end: FUTURE_TIMESTAMP,
            extra: mockExtra,
          },
          mockLaoId,
        );
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if start is undefined', () => {
      const createWrongObj = () =>
        new CreateMeeting(
          {
            object: ObjectType.MEETING,
            action: ActionType.CREATE,
            id: mockMeetingId,
            name: NAME,
            creation: TIMESTAMP,
            location: LOCATION,
            end: FUTURE_TIMESTAMP,
            extra: mockExtra,
          },
          mockLaoId,
        );
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if id is undefined', () => {
      const createWrongObj = () =>
        new CreateMeeting(
          {
            object: ObjectType.MEETING,
            action: ActionType.CREATE,
            name: NAME,
            creation: TIMESTAMP,
            location: LOCATION,
            start: TIMESTAMP,
            end: FUTURE_TIMESTAMP,
            extra: mockExtra,
          },
          mockLaoId,
        );
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if end is before creation', () => {
      const TIMESTAMP_BEFORE = new Timestamp(1609445600);
      const createWrongObj = () =>
        new CreateMeeting(
          {
            object: ObjectType.MEETING,
            action: ActionType.CREATE,
            id: mockMeetingId,
            name: NAME,
            creation: TIMESTAMP,
            location: LOCATION,
            start: TIMESTAMP,
            end: TIMESTAMP_BEFORE,
            extra: mockExtra,
          },
          mockLaoId,
        );
      expect(createWrongObj).toThrow(ProtocolError);
    });
  });

  describe('validate', () => {
    it('should succeed if id is correct', () => {
      expect(
        () =>
          new CreateMeeting(
            {
              object: ObjectType.MEETING,
              action: ActionType.CREATE,
              id: mockMeetingId,
              name: NAME,
              creation: TIMESTAMP,
              location: LOCATION,
              start: TIMESTAMP,
              end: FUTURE_TIMESTAMP,
              extra: mockExtra,
            },
            mockLaoId,
          ),
      ).not.toThrow();
    });

    it('should throw an error if id is incorrect', () => {
      expect(
        () =>
          new CreateMeeting(
            {
              object: ObjectType.MEETING,
              action: ActionType.CREATE,
              id: new Hash('id'),
              name: NAME,
              creation: TIMESTAMP,
              location: LOCATION,
              start: TIMESTAMP,
              end: FUTURE_TIMESTAMP,
              extra: mockExtra,
            },
            mockLaoId,
          ),
      ).toThrow(ProtocolError);
    });
  });
});
