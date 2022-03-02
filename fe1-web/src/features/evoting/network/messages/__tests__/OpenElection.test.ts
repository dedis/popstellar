import 'jest-extended';
import '__tests__/utils/matchers';
import {
  mockLao,
  mockLaoId,
  mockLaoIdHash,
  mockLaoName,
  configureTestFeatures,
} from '__tests__/utils';

import { Hash, Timestamp, ProtocolError } from 'core/objects';
import { ActionType, ObjectType } from 'core/network/jsonrpc/messages';
import { OpenedLaoStore } from 'features/lao/store';

import { MessageDataProperties } from 'core/types';
import { OpenElection } from '../OpenElection';

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
const sampleOpenElection: Partial<OpenElection> = {
  object: ObjectType.ELECTION,
  action: ActionType.OPEN,
  election: electionId,
  lao: mockLaoIdHash,
  opened_at: TIMESTAMP,
};

const openElectionJson: string = `{
  "object": "${ObjectType.ELECTION}",
  "action": "${ActionType.OPEN}",
  "election": "${electionId}",
  "lao": "${mockLaoIdHash}",
  "opened_at": ${TIMESTAMP}
}`;

beforeAll(() => {
  configureTestFeatures();
  OpenedLaoStore.store(mockLao);
});

describe('OpenElection', () => {
  it('should be created correctly from Json', () => {
    expect(
      new OpenElection(sampleOpenElection as MessageDataProperties<OpenElection>),
    ).toBeJsonEqual(sampleOpenElection);
    const temp = {
      object: ObjectType.ELECTION,
      action: ActionType.OPEN,
      election: electionId,
      lao: mockLaoIdHash,
      opened_at: TIMESTAMP,
    };
    expect(new OpenElection(temp)).toBeJsonEqual(temp);
  });

  it('should be parsed correctly from Json', () => {
    const obj = JSON.parse(openElectionJson);
    expect(OpenElection.fromJson(obj)).toBeJsonEqual(sampleOpenElection);
  });

  it('fromJson should throw an error if the Json has incorrect action', () => {
    const obj = {
      object: ObjectType.ELECTION,
      action: ActionType.NOTIFY_ADD,
      election: electionId.toString(),
      lao: mockLaoIdHash.toString(),
      opened_at: parseInt(TIMESTAMP.toString(), 10),
    };
    const createFromJson = () => OpenElection.fromJson(obj);
    expect(createFromJson).toThrow(ProtocolError);
  });

  it('fromJson should throw an error if the Json has incorrect object', () => {
    const obj = {
      object: ObjectType.CHIRP,
      action: ActionType.OPEN,
      election: electionId.toString(),
      lao: mockLaoIdHash.toString(),
      opened_at: parseInt(TIMESTAMP.toString(), 10),
    };
    const createFromJson = () => OpenElection.fromJson(obj);
    expect(createFromJson).toThrow(ProtocolError);
  });

  describe('constructor', () => {
    it('should throw an error if election is undefined', () => {
      const createWrongObj = () =>
        // @ts-ignore. Here was pass data to the constructor that is missing a field even though ts would require it
        new OpenElection({
          lao: mockLaoIdHash,
          opened_at: TIMESTAMP,
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if lao is undefined', () => {
      const createWrongObj = () =>
        // @ts-ignore. Here was pass data to the constructor that is missing a field even though ts would require it
        new OpenElection({
          election: electionId,
          opened_at: TIMESTAMP,
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if opened_at is undefined', () => {
      const createWrongObj = () =>
        // @ts-ignore. Here was pass data to the constructor that is missing a field even though ts would require it
        new OpenElection({
          election: electionId,
          lao: mockLaoIdHash,
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should ignore passed object and action parameters', () => {
      const msg = new OpenElection({
        // @ts-ignore. Here we pass additional fields to the constructor that should not be set
        object: ObjectType.CHIRP,
        action: ActionType.NOTIFY_ADD,
        election: electionId,
        lao: mockLaoIdHash,
        opened_at: TIMESTAMP,
      });

      expect(msg.object).toEqual(ObjectType.ELECTION);
      expect(msg.action).toEqual(ActionType.OPEN);
    });
  });
});
