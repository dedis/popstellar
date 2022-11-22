import 'jest-extended';
import '__tests__/utils/matchers';

import { configureTestFeatures, mockLao, serializedMockLaoId } from '__tests__/utils';
import { ActionType, ObjectType } from 'core/network/jsonrpc/messages';
import { Base64UrlData, Hash, ProtocolError, Timestamp } from 'core/objects';
import { OpenedLaoStore } from 'features/lao/store';

import { StateMeeting } from '../StateMeeting';

const NAME = 'myMeeting';
const LOCATION = 'location';
const TIMESTAMP = new Timestamp(1609455600); // 1st january 2021
const FUTURE_TIMESTAMP = new Timestamp(1735686000); // 1st january 2025
const TIMESTAMP_BEFORE = new Timestamp(1609445600);
const mockMeetingId = Hash.fromStringArray('M', serializedMockLaoId, TIMESTAMP.toString(), NAME);
const mockMessageId = Base64UrlData.encode('message_id');
const mockExtra = { extra: 'extra info' };
const mockModificationId = Hash.fromStringArray(mockMessageId.toString());

const sampleStateMeeting: Partial<StateMeeting> = {
  object: ObjectType.MEETING,
  action: ActionType.STATE,
  id: mockMeetingId,
  name: NAME,
  creation: TIMESTAMP,
  last_modified: TIMESTAMP,
  location: LOCATION,
  start: TIMESTAMP,
  end: FUTURE_TIMESTAMP,
  extra: mockExtra,
  modification_id: mockModificationId,
  modification_signatures: [],
};

const stateMeetingJson = `{
  "object": "${ObjectType.MEETING}",
  "action": "${ActionType.STATE}",
  "id": "${mockMeetingId}",
  "name": "${NAME}",
  "creation": ${TIMESTAMP},
  "last_modified": ${TIMESTAMP},
  "location": "${LOCATION}",
  "start": ${TIMESTAMP},
  "end": ${FUTURE_TIMESTAMP},
  "extra": ${JSON.stringify(mockExtra)},
  "modification_id": "${mockModificationId}",
  "modification_signatures": []
}`;

beforeAll(() => {
  configureTestFeatures();
  OpenedLaoStore.store(mockLao);
});

describe('StateMeeting', () => {
  it('should be created correctly from Json', () => {
    expect(new StateMeeting(sampleStateMeeting)).toBeJsonEqual(sampleStateMeeting);
    const temp = {
      object: ObjectType.MEETING,
      action: ActionType.STATE,
      id: mockMeetingId,
      name: NAME,
      creation: TIMESTAMP,
      last_modified: TIMESTAMP,
      location: LOCATION,
      start: TIMESTAMP,
      end: FUTURE_TIMESTAMP,
      extra: mockExtra,
      modification_id: mockModificationId,
      modification_signatures: [],
    };
    expect(new StateMeeting(temp)).toBeJsonEqual(temp);
  });

  it('should be parsed correctly from Json', () => {
    const obj = JSON.parse(stateMeetingJson);
    expect(StateMeeting.fromJson(obj)).toBeJsonEqual(sampleStateMeeting);
  });

  it('fromJson should throw an error if the Json has incorrect action', () => {
    const obj = {
      object: ObjectType.MEETING,
      action: ActionType.STATE,
      id: mockMeetingId,
      name: NAME,
      creation: TIMESTAMP,
      last_modified: TIMESTAMP,
      location: LOCATION,
      start: TIMESTAMP,
      end: FUTURE_TIMESTAMP,
      extra: mockExtra,
      modification_id: mockModificationId,
      modification_signatures: [],
    };
    const createWrongObj = () => StateMeeting.fromJson(obj);
    expect(createWrongObj).toThrow(ProtocolError);
  });

  describe('constructor', () => {
    it('should throw an error when name is undefined', () => {
      const createWrongObj = () =>
        new StateMeeting({
          object: ObjectType.MEETING,
          action: ActionType.STATE,
          id: mockMeetingId,
          creation: TIMESTAMP,
          last_modified: TIMESTAMP,
          location: LOCATION,
          start: TIMESTAMP,
          end: FUTURE_TIMESTAMP,
          extra: mockExtra,
          modification_id: mockModificationId,
          modification_signatures: [],
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error when creation is undefined', () => {
      const createWrongObj = () =>
        new StateMeeting({
          object: ObjectType.MEETING,
          action: ActionType.STATE,
          id: mockMeetingId,
          name: NAME,
          last_modified: TIMESTAMP,
          location: LOCATION,
          start: TIMESTAMP,
          end: FUTURE_TIMESTAMP,
          extra: mockExtra,
          modification_id: mockModificationId,
          modification_signatures: [],
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error when last_modified is undefined', () => {
      const createWrongObj = () =>
        new StateMeeting({
          object: ObjectType.MEETING,
          action: ActionType.STATE,
          id: mockMeetingId,
          name: NAME,
          creation: TIMESTAMP,
          location: LOCATION,
          start: TIMESTAMP,
          end: FUTURE_TIMESTAMP,
          extra: mockExtra,
          modification_id: mockModificationId,
          modification_signatures: [],
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error when start is undefined', () => {
      const createWrongObj = () =>
        new StateMeeting({
          object: ObjectType.MEETING,
          action: ActionType.STATE,
          id: mockMeetingId,
          name: NAME,
          creation: TIMESTAMP,
          last_modified: TIMESTAMP,
          location: LOCATION,
          end: FUTURE_TIMESTAMP,
          extra: mockExtra,
          modification_id: mockModificationId,
          modification_signatures: [],
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error when modification_id is undefined', () => {
      const createWrongObj = () =>
        new StateMeeting({
          object: ObjectType.MEETING,
          action: ActionType.STATE,
          id: mockMeetingId,
          name: NAME,
          creation: TIMESTAMP,
          last_modified: TIMESTAMP,
          location: LOCATION,
          start: TIMESTAMP,
          end: FUTURE_TIMESTAMP,
          extra: mockExtra,
          modification_signatures: [],
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error when modification_signatures is undefined', () => {
      const createWrongObj = () =>
        new StateMeeting({
          object: ObjectType.MEETING,
          action: ActionType.STATE,
          id: mockMeetingId,
          name: NAME,
          creation: TIMESTAMP,
          last_modified: TIMESTAMP,
          location: LOCATION,
          start: TIMESTAMP,
          end: FUTURE_TIMESTAMP,
          extra: mockExtra,
          modification_id: mockModificationId,
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error when id is undefined', () => {
      const createWrongObj = () =>
        new StateMeeting({
          object: ObjectType.MEETING,
          action: ActionType.STATE,
          name: NAME,
          creation: TIMESTAMP,
          last_modified: TIMESTAMP,
          location: LOCATION,
          start: TIMESTAMP,
          end: FUTURE_TIMESTAMP,
          extra: mockExtra,
          modification_id: mockModificationId,
          modification_signatures: [],
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error when last_modified is before creation', () => {
      const createWrongObj = () =>
        new StateMeeting({
          object: ObjectType.MEETING,
          action: ActionType.STATE,
          id: mockMeetingId,
          name: NAME,
          creation: TIMESTAMP,
          last_modified: TIMESTAMP_BEFORE,
          location: LOCATION,
          start: TIMESTAMP,
          end: TIMESTAMP_BEFORE,
          extra: mockExtra,
          modification_id: mockModificationId,
          modification_signatures: [],
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error when end is before creation', () => {
      const createWrongObj = () =>
        new StateMeeting({
          object: ObjectType.MEETING,
          action: ActionType.STATE,
          id: mockMeetingId,
          name: NAME,
          creation: TIMESTAMP,
          last_modified: TIMESTAMP,
          location: LOCATION,
          start: TIMESTAMP_BEFORE,
          end: TIMESTAMP_BEFORE,
          extra: mockExtra,
          modification_id: mockModificationId,
          modification_signatures: [],
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error when end is before start', () => {
      const createWrongObj = () =>
        new StateMeeting({
          object: ObjectType.MEETING,
          action: ActionType.STATE,
          id: mockMeetingId,
          name: NAME,
          creation: TIMESTAMP_BEFORE,
          last_modified: TIMESTAMP_BEFORE,
          location: LOCATION,
          start: TIMESTAMP,
          end: TIMESTAMP_BEFORE,
          extra: mockExtra,
          modification_id: mockModificationId,
          modification_signatures: [],
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });
  });
});
