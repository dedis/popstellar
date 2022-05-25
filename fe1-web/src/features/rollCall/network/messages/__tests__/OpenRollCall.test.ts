import 'jest-extended';
import '__tests__/utils/matchers';

import { configureTestFeatures, mockLaoId, mockLaoIdHash, mockLaoName } from '__tests__/utils';
import { ActionType, ObjectType } from 'core/network/jsonrpc/messages';
import { Hash, ProtocolError, Timestamp } from 'core/objects';

import { OpenRollCall } from '../OpenRollCall';

const TIMESTAMP = new Timestamp(1609455600); // 1st january 2021
const rollCallId = Hash.fromStringArray('R', mockLaoId, TIMESTAMP.toString(), mockLaoName);
const rollCallUpdateId = Hash.fromStringArray(
  'R',
  mockLaoId,
  rollCallId.toString(),
  TIMESTAMP.toString(),
);

const sampleOpenRollCall: Partial<OpenRollCall> = {
  object: ObjectType.ROLL_CALL,
  action: ActionType.OPEN,
  update_id: rollCallUpdateId,
  opens: rollCallId,
  opened_at: TIMESTAMP,
};

const openRollCallJson = `{
  "object": "${ObjectType.ROLL_CALL}",
  "action": "${ActionType.OPEN}",
  "update_id": "${rollCallUpdateId}",
  "opens": "${rollCallId}",
  "opened_at": ${TIMESTAMP}
}`;

beforeAll(() => {
  configureTestFeatures();
});

describe('OpenRollCall', () => {
  it('should be created correctly from Json', () => {
    expect(new OpenRollCall(sampleOpenRollCall, mockLaoIdHash)).toBeJsonEqual(sampleOpenRollCall);
    const temp = {
      object: ObjectType.ROLL_CALL,
      action: ActionType.OPEN,
      update_id: rollCallUpdateId,
      opens: rollCallId,
      opened_at: TIMESTAMP,
    };
    expect(new OpenRollCall(temp, mockLaoIdHash)).toBeJsonEqual(temp);
  });

  it('should be parsed correctly from Json', () => {
    const obj = JSON.parse(openRollCallJson);
    expect(OpenRollCall.fromJson(obj, mockLaoIdHash)).toBeJsonEqual(sampleOpenRollCall);
  });

  it('fromJson should throw an error if the Json has incorrect action', () => {
    const obj = {
      object: ObjectType.ROLL_CALL,
      action: ActionType.CREATE,
      update_id: rollCallUpdateId,
      opens: rollCallId,
      opened_at: TIMESTAMP,
    };
    const createWrongObj = () => OpenRollCall.fromJson(obj, mockLaoIdHash);
    expect(createWrongObj).toThrow(ProtocolError);
  });

  describe('constructor', () => {
    it('should throw an error if opened_at is undefined', () => {
      const createWrongObj = () =>
        new OpenRollCall(
          {
            object: ObjectType.ROLL_CALL,
            action: ActionType.CREATE,
            update_id: rollCallUpdateId,
            opens: rollCallId,
          },
          mockLaoIdHash,
        );
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if opens is undefined', () => {
      const createWrongObj = () =>
        new OpenRollCall(
          {
            object: ObjectType.ROLL_CALL,
            action: ActionType.CREATE,
            update_id: rollCallUpdateId,
            opened_at: TIMESTAMP,
          },
          mockLaoIdHash,
        );
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if update_id is undefined', () => {
      const createWrongObj = () =>
        new OpenRollCall(
          {
            object: ObjectType.ROLL_CALL,
            action: ActionType.CREATE,
            opens: rollCallId,
            opened_at: TIMESTAMP,
          },
          mockLaoIdHash,
        );
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if update_id is incorrect', () => {
      const createWrongObj = () =>
        new OpenRollCall(
          {
            object: ObjectType.ROLL_CALL,
            action: ActionType.CREATE,
            update_id: new Hash('id'),
            opens: rollCallId,
            opened_at: TIMESTAMP,
          },
          mockLaoIdHash,
        );
      expect(createWrongObj).toThrow(ProtocolError);
    });
  });
});
