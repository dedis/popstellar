import 'jest-extended';
import '__tests__/utils/matchers';

import {
  configureTestFeatures,
  mockLaoId,
  mockLaoName,
  mockPublicKey,
  mockPublicKey2,
} from '__tests__/utils';
import { ActionType, ObjectType } from 'core/network/jsonrpc/messages';
import { Hash, ProtocolError, PublicKey, Timestamp } from 'core/objects';

import { CloseRollCall } from '../CloseRollCall';

const TIMESTAMP = new Timestamp(1609455600); // 1st january 2021
const rollCallId = Hash.fromArray('R', mockLaoId, TIMESTAMP, mockLaoName);
const rollCallCloseId = Hash.fromArray('R', mockLaoId, rollCallId, TIMESTAMP);
const mockAttendees = [new PublicKey(mockPublicKey2), new PublicKey(mockPublicKey)];

const sampleCloseRollCall: Partial<CloseRollCall> = {
  object: ObjectType.ROLL_CALL,
  action: ActionType.CLOSE,
  update_id: rollCallCloseId,
  closes: rollCallId,
  closed_at: TIMESTAMP,
  attendees: mockAttendees,
};

const closeRollCallJson = `{
  "object": "${ObjectType.ROLL_CALL}",
  "action": "${ActionType.CLOSE}",
  "update_id": "${rollCallCloseId}",
  "closes": "${rollCallId}",
  "closed_at": ${TIMESTAMP},
  "attendees": ${JSON.stringify(mockAttendees)}
}`;

beforeAll(() => {
  configureTestFeatures();
});

describe('CloseRollCall', () => {
  it('should be created correctly from Json', () => {
    expect(new CloseRollCall(sampleCloseRollCall, mockLaoId)).toBeJsonEqual(sampleCloseRollCall);
    const temp = {
      object: ObjectType.ROLL_CALL,
      action: ActionType.CLOSE,
      update_id: rollCallCloseId,
      closes: rollCallId,
      closed_at: TIMESTAMP,
      attendees: mockAttendees,
    };
    expect(new CloseRollCall(temp, mockLaoId)).toBeJsonEqual(temp);
  });

  it('should be parsed correctly from Json', () => {
    const obj = JSON.parse(closeRollCallJson);
    expect(CloseRollCall.fromJson(obj, mockLaoId)).toBeJsonEqual(sampleCloseRollCall);
  });

  it('fromJson should throw an error if the Json has incorrect action', () => {
    const obj = {
      object: ObjectType.ROLL_CALL,
      action: ActionType.DELETE,
      update_id: rollCallCloseId,
      closes: rollCallId,
      closed_at: TIMESTAMP,
      attendees: mockAttendees,
    };
    const createWrongObj = () => CloseRollCall.fromJson(obj, mockLaoId);
    expect(createWrongObj).toThrow(ProtocolError);
  });

  describe('constructor', () => {
    it('should throw an error if closed_at is undefined', () => {
      const createWrongObj = () =>
        new CloseRollCall(
          {
            object: ObjectType.ROLL_CALL,
            action: ActionType.DELETE,
            update_id: rollCallCloseId,
            closes: rollCallId,
            attendees: mockAttendees,
          },
          mockLaoId,
        );
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if attendees is undefined', () => {
      const createWrongObj = () =>
        new CloseRollCall(
          {
            object: ObjectType.ROLL_CALL,
            action: ActionType.DELETE,
            update_id: rollCallCloseId,
            closes: rollCallId,
            closed_at: TIMESTAMP,
          },
          mockLaoId,
        );
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if closes is undefined', () => {
      const createWrongObj = () =>
        new CloseRollCall(
          {
            object: ObjectType.ROLL_CALL,
            action: ActionType.DELETE,
            update_id: rollCallCloseId,
            closed_at: TIMESTAMP,
            attendees: mockAttendees,
          },
          mockLaoId,
        );
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if update_id is undefined', () => {
      const createWrongObj = () =>
        new CloseRollCall(
          {
            object: ObjectType.ROLL_CALL,
            action: ActionType.DELETE,
            closes: rollCallId,
            closed_at: TIMESTAMP,
            attendees: mockAttendees,
          },
          mockLaoId,
        );
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if update_id is invalid', () => {
      const createWrongObj = () =>
        new CloseRollCall(
          {
            object: ObjectType.ROLL_CALL,
            action: ActionType.DELETE,
            update_id: new Hash('id'),
            closes: rollCallId,
            closed_at: TIMESTAMP,
            attendees: mockAttendees,
          },
          mockLaoId,
        );
      expect(createWrongObj).toThrow(ProtocolError);
    });
  });
});
