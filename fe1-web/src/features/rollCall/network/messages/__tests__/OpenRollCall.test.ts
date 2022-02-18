import 'jest-extended';

import '__tests__/utils/matchers';
import { ActionType, ObjectType } from 'core/network/jsonrpc/messages/MessageData';
import { Hash, Timestamp, ProtocolError } from 'core/objects';
import { mockLao, mockLaoId, mockLaoName } from '__tests__/utils/TestUtils';
import { OpenedLaoStore } from 'features/lao/store';

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
  OpenedLaoStore.store(mockLao);
});

describe('OpenRollCall', () => {
  it('should be created correctly from Json', () => {
    expect(new OpenRollCall(sampleOpenRollCall)).toBeJsonEqual(sampleOpenRollCall);
    const temp = {
      object: ObjectType.ROLL_CALL,
      action: ActionType.OPEN,
      update_id: rollCallUpdateId,
      opens: rollCallId,
      opened_at: TIMESTAMP,
    };
    expect(new OpenRollCall(temp)).toBeJsonEqual(temp);
  });

  it('should be parsed correctly from Json', () => {
    const obj = JSON.parse(openRollCallJson);
    expect(OpenRollCall.fromJson(obj)).toBeJsonEqual(sampleOpenRollCall);
  });

  it('fromJson should throw an error if the Json has incorrect action', () => {
    const obj = {
      object: ObjectType.ROLL_CALL,
      action: ActionType.CREATE,
      update_id: rollCallUpdateId,
      opens: rollCallId,
      opened_at: TIMESTAMP,
    };
    const createWrongObj = () => OpenRollCall.fromJson(obj);
    expect(createWrongObj).toThrow(ProtocolError);
  });

  describe('constructor', () => {
    it('should throw an error if opened_at is undefined', () => {
      const createWrongObj = () =>
        new OpenRollCall({
          object: ObjectType.ROLL_CALL,
          action: ActionType.CREATE,
          update_id: rollCallUpdateId,
          opens: rollCallId,
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if opens is undefined', () => {
      const createWrongObj = () =>
        new OpenRollCall({
          object: ObjectType.ROLL_CALL,
          action: ActionType.CREATE,
          update_id: rollCallUpdateId,
          opened_at: TIMESTAMP,
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if update_id is undefined', () => {
      const createWrongObj = () =>
        new OpenRollCall({
          object: ObjectType.ROLL_CALL,
          action: ActionType.CREATE,
          opens: rollCallId,
          opened_at: TIMESTAMP,
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if update_id is incorrect', () => {
      const createWrongObj = () =>
        new OpenRollCall({
          object: ObjectType.ROLL_CALL,
          action: ActionType.CREATE,
          update_id: new Hash('id'),
          opens: rollCallId,
          opened_at: TIMESTAMP,
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });
  });
});
