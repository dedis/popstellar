import 'jest-extended';
import '__tests__/utils/matchers';
import { Hash, Timestamp } from 'model/objects';
import { mockLao, mockLaoId, mockLaoName } from '__tests__/utils/TestUtils';
import { ActionType, ObjectType } from 'model/network/method/message/data/MessageData';
import { ProtocolError } from 'model/network/ProtocolError';
import { OpenedLaoStore } from 'store';
import { ReopenRollCall } from '../ReopenRollCall';

const TIMESTAMP = new Timestamp(1609455600); // 1st january 2021
const rollCallId = Hash.fromStringArray('R', mockLaoId, TIMESTAMP.toString(), mockLaoName);
const rollCallUpdateId = Hash.fromStringArray('R', mockLaoId, rollCallId.toString(),
  TIMESTAMP.toString());

const sampleReopenRollCall: Partial<ReopenRollCall> = {
  object: ObjectType.ROLL_CALL,
  action: ActionType.REOPEN,
  update_id: rollCallUpdateId,
  opens: rollCallId,
  opened_at: TIMESTAMP,
};

const reopenRollCallJson = `{
  "object": "${ObjectType.ROLL_CALL}",
  "action": "${ActionType.REOPEN}",
  "update_id": "${rollCallUpdateId}",
  "opens": "${rollCallId}",
  "opened_at": ${TIMESTAMP}
}`;

beforeAll(() => {
  OpenedLaoStore.store(mockLao);
});

describe('ReopenRollCall', () => {
  it('should be created correctly from Json', () => {
    expect(new ReopenRollCall(sampleReopenRollCall)).toBeJsonEqual(sampleReopenRollCall);
    const temp = {
      object: ObjectType.ROLL_CALL,
      action: ActionType.REOPEN,
      update_id: rollCallUpdateId,
      opens: rollCallId,
      opened_at: TIMESTAMP,
    };
    expect(new ReopenRollCall(temp)).toBeJsonEqual(temp);
  });

  it('should be parsed correctly from Json', () => {
    const obj = JSON.parse(reopenRollCallJson);
    expect(ReopenRollCall.fromJson(obj)).toBeJsonEqual(sampleReopenRollCall);
  });

  it('fromJson should throw an error if the Json has incorrect action', () => {
    const obj = {
      object: ObjectType.ROLL_CALL,
      action: ActionType.ADD,
      update_id: rollCallUpdateId,
      opens: rollCallId,
      opened_at: TIMESTAMP,
    };
    const createWrongObj = () => ReopenRollCall.fromJson(obj);
    expect(createWrongObj).toThrow(ProtocolError);
  });
});
