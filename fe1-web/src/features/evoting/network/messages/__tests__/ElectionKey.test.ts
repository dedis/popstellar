import 'jest-extended';
import '__tests__/utils/matchers';

import {
  configureTestFeatures,
  mockKeyPair,
  mockLaoId,
  mockLaoName,
  mockPublicKey,
} from '__tests__/utils';
import { ActionType, ObjectType } from 'core/network/jsonrpc/messages';
import { Hash, ProtocolError, PublicKey, Timestamp } from 'core/objects';
import { MessageDataProperties } from 'core/types';

import { ElectionKey } from '../ElectionKey';

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
const sampleElectionKey: Partial<ElectionKey> = {
  object: ObjectType.ELECTION,
  action: ActionType.KEY,
  election: electionId,
  election_key: mockKeyPair.publicKey,
};

const ElectionKeyJson: string = `{
  "object": "${ObjectType.ELECTION}",
  "action": "${ActionType.KEY}",
  "election": "${electionId}",
  "election_key": "${mockPublicKey}"
}`;

// endregion

beforeAll(() => {
  configureTestFeatures();
});

describe('ElectionKey', () => {
  it('should be created correctly from Json', () => {
    expect(new ElectionKey(sampleElectionKey as MessageDataProperties<ElectionKey>)).toBeJsonEqual(
      sampleElectionKey,
    );
    const temp = {
      object: ObjectType.ELECTION,
      action: ActionType.KEY,
      election: electionId,
      election_key: mockKeyPair.publicKey,
    };
    expect(new ElectionKey(temp)).toBeJsonEqual(temp);
  });

  it('should be parsed correctly from Json', () => {
    const obj = JSON.parse(ElectionKeyJson);
    expect(ElectionKey.fromJson(obj)).toBeJsonEqual(sampleElectionKey);
  });

  it('fromJson should throw an error if the Json has incorrect action', () => {
    const obj = {
      object: ObjectType.ELECTION,
      action: ActionType.NOTIFY_ADD,
      election: electionId.toString(),
      election_key: mockPublicKey,
    };
    const createFromJson = () => ElectionKey.fromJson(obj);
    expect(createFromJson).toThrow(ProtocolError);
  });

  it('fromJson should throw an error if the Json has incorrect object', () => {
    const obj = {
      object: ObjectType.CHIRP,
      action: ActionType.KEY,
      election: electionId.toString(),
      election_key: mockPublicKey,
    };
    const createFromJson = () => ElectionKey.fromJson(obj);
    expect(createFromJson).toThrow(ProtocolError);
  });

  describe('constructor', () => {
    it('should throw an error if election is undefined', () => {
      const createWrongObj = () =>
        new ElectionKey({
          election: undefined as unknown as Hash,
          election_key: mockKeyPair.publicKey,
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if electionKey is undefined', () => {
      const createWrongObj = () =>
        new ElectionKey({
          election: electionId,
          election_key: undefined as unknown as PublicKey,
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should ignore passed object and action parameters', () => {
      const msg = new ElectionKey({
        object: ObjectType.CHIRP,
        action: ActionType.NOTIFY_ADD,
        election: electionId,
        election_key: mockKeyPair.publicKey,
      } as MessageDataProperties<ElectionKey>);

      expect(msg.object).toEqual(ObjectType.ELECTION);
      expect(msg.action).toEqual(ActionType.KEY);
    });
  });
});
