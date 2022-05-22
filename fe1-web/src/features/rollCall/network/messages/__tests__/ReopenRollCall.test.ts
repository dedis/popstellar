import 'jest-extended';
import '__tests__/utils/matchers';

import {
  configureTestFeatures,
  mockLao,
  mockLaoId,
  mockLaoIdHash,
  mockLaoName,
} from '__tests__/utils';
import { ActionType, ObjectType } from 'core/network/jsonrpc/messages';
import { Hash, ProtocolError, Timestamp } from 'core/objects';
import { OpenedLaoStore } from 'features/lao/store';

import { ReopenRollCall } from '../ReopenRollCall';

const TIMESTAMP = new Timestamp(1609455600); // 1st january 2021
const rollCallId = Hash.fromStringArray('R', mockLaoId, TIMESTAMP.toString(), mockLaoName);
const rollCallUpdateId = Hash.fromStringArray(
  'R',
  mockLaoId,
  rollCallId.toString(),
  TIMESTAMP.toString(),
);

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
  configureTestFeatures();
  OpenedLaoStore.store(mockLao);
});

describe('ReopenRollCall', () => {
  it('should be created correctly from Json', () => {
    expect(new ReopenRollCall(sampleReopenRollCall, mockLaoIdHash)).toBeJsonEqual(
      sampleReopenRollCall,
    );
    const temp = {
      object: ObjectType.ROLL_CALL,
      action: ActionType.REOPEN,
      update_id: rollCallUpdateId,
      opens: rollCallId,
      opened_at: TIMESTAMP,
    };
    expect(new ReopenRollCall(temp, mockLaoIdHash)).toBeJsonEqual(temp);
  });

  it('should be parsed correctly from Json', () => {
    const obj = JSON.parse(reopenRollCallJson);
    expect(ReopenRollCall.fromJson(obj, mockLaoIdHash)).toBeJsonEqual(sampleReopenRollCall);
  });

  it('fromJson should throw an error if the Json has incorrect action', () => {
    const obj = {
      object: ObjectType.ROLL_CALL,
      action: ActionType.ADD,
      update_id: rollCallUpdateId,
      opens: rollCallId,
      opened_at: TIMESTAMP,
    };
    const createWrongObj = () => ReopenRollCall.fromJson(obj, mockLaoIdHash);
    expect(createWrongObj).toThrow(ProtocolError);
  });
});
