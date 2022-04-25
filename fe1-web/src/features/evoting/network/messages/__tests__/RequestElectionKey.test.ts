import 'jest-extended';
import '__tests__/utils/matchers';

import { configureTestFeatures, mockLaoId, mockLaoName } from '__tests__/utils';
import { ActionType, ObjectType } from 'core/network/jsonrpc/messages';
import { Hash, ProtocolError, Timestamp } from 'core/objects';
import { MessageDataProperties } from 'core/types';

import { RequestElectionKey } from '../RequestElectionKey';

// region test data initialization

const TIMESTAMP = new Timestamp(1609455600); // 1st january 2021

const electionId: Hash = Hash.fromStringArray(
  'Election',
  mockLaoId,
  TIMESTAMP.toString(),
  mockLaoName,
);

// In these tests, we should assume that the input to the messages is
// just a Partial<> and not a MessageDataProperties<>
// as this will catch more issues at runtime. (Defensive programming)
const sampleRequestElectionKey: Partial<RequestElectionKey> = {
  object: ObjectType.ELECTION,
  action: ActionType.REQUEST_KEY,
  election: electionId,
};

const RequestElectionKeyJson: string = `{
  "object": "${ObjectType.ELECTION}",
  "action": "${ActionType.REQUEST_KEY}",
  "election": "${electionId}"
}`;

// endregion

beforeAll(() => {
  configureTestFeatures();
});

describe('RequestElectionKey', () => {
  it('should be created correctly from Json', () => {
    expect(
      new RequestElectionKey(sampleRequestElectionKey as MessageDataProperties<RequestElectionKey>),
    ).toBeJsonEqual(sampleRequestElectionKey);
    const temp = {
      object: ObjectType.ELECTION,
      action: ActionType.REQUEST_KEY,
      election: electionId,
    };
    expect(new RequestElectionKey(temp)).toBeJsonEqual(temp);
  });

  it('should be parsed correctly from Json', () => {
    const obj = JSON.parse(RequestElectionKeyJson);
    expect(RequestElectionKey.fromJson(obj)).toBeJsonEqual(sampleRequestElectionKey);
  });

  it('fromJson should throw an error if the Json has incorrect action', () => {
    const obj = {
      object: ObjectType.ELECTION,
      action: ActionType.NOTIFY_ADD,
      election: electionId.toString(),
    };
    const createFromJson = () => RequestElectionKey.fromJson(obj);
    expect(createFromJson).toThrow(ProtocolError);
  });

  it('fromJson should throw an error if the Json has incorrect object', () => {
    const obj = {
      object: ObjectType.CHIRP,
      action: ActionType.REQUEST_KEY,
      election: electionId.toString(),
    };
    const createFromJson = () => RequestElectionKey.fromJson(obj);
    expect(createFromJson).toThrow(ProtocolError);
  });

  describe('constructor', () => {
    it('should throw an error if election is undefined', () => {
      const createWrongObj = () =>
        new RequestElectionKey({
          election: undefined as unknown as Hash,
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should ignore passed object and action parameters', () => {
      const msg = new RequestElectionKey({
        object: ObjectType.CHIRP,
        action: ActionType.NOTIFY_ADD,
        election: electionId,
      } as MessageDataProperties<RequestElectionKey>);

      expect(msg.object).toEqual(ObjectType.ELECTION);
      expect(msg.action).toEqual(ActionType.REQUEST_KEY);
    });
  });
});
