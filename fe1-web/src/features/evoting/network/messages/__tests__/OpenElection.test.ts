import 'jest-extended';
import '__tests__/utils/matchers';

import { configureTestFeatures, mockLaoId } from '__tests__/utils';
import { ActionType, ObjectType } from 'core/network/jsonrpc/messages';
import { Hash, ProtocolError, Timestamp } from 'core/objects';
import { MessageDataProperties } from 'core/types';
import { mockElectionId } from 'features/evoting/__tests__/utils';

import { OpenElection } from '../OpenElection';

const TIMESTAMP = new Timestamp(1609455600); // 1st january 2021

// In these tests, we should assume that the input to the messages is
// just a Partial<> and not a MessageDataProperties<>
// as this will catch more issues at runtime. (Defensive programming)
const sampleOpenElection: Partial<OpenElection> = {
  object: ObjectType.ELECTION,
  action: ActionType.OPEN,
  election: mockElectionId,
  lao: mockLaoId,
  opened_at: TIMESTAMP,
};

const openElectionJson: string = `{
  "object": "${ObjectType.ELECTION}",
  "action": "${ActionType.OPEN}",
  "election": "${mockElectionId}",
  "lao": "${mockLaoId}",
  "opened_at": ${TIMESTAMP}
}`;

beforeAll(() => {
  configureTestFeatures();
});

describe('OpenElection', () => {
  it('should be created correctly from Json', () => {
    expect(
      new OpenElection(sampleOpenElection as MessageDataProperties<OpenElection>),
    ).toBeJsonEqual(sampleOpenElection);
    const temp = {
      object: ObjectType.ELECTION,
      action: ActionType.OPEN,
      election: mockElectionId,
      lao: mockLaoId,
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
      election: mockElectionId.toString(),
      lao: mockLaoId.toString(),
      opened_at: TIMESTAMP.valueOf(),
    };
    const createFromJson = () => OpenElection.fromJson(obj);
    expect(createFromJson).toThrow(ProtocolError);
  });

  it('fromJson should throw an error if the Json has incorrect object', () => {
    const obj = {
      object: ObjectType.CHIRP,
      action: ActionType.OPEN,
      election: mockElectionId.toString(),
      lao: mockLaoId.toString(),
      opened_at: TIMESTAMP.valueOf(),
    };
    const createFromJson = () => OpenElection.fromJson(obj);
    expect(createFromJson).toThrow(ProtocolError);
  });

  describe('constructor', () => {
    it('should throw an error if election is undefined', () => {
      const createWrongObj = () =>
        new OpenElection({
          lao: mockLaoId,
          election: undefined as unknown as Hash,
          opened_at: TIMESTAMP,
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if lao is undefined', () => {
      const createWrongObj = () =>
        new OpenElection({
          lao: undefined as unknown as Hash,
          election: mockElectionId,
          opened_at: TIMESTAMP,
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if opened_at is undefined', () => {
      const createWrongObj = () =>
        new OpenElection({
          election: mockElectionId,
          lao: mockLaoId,
          opened_at: undefined as unknown as Timestamp,
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should ignore passed object and action parameters', () => {
      const msg = new OpenElection({
        object: ObjectType.CHIRP,
        action: ActionType.NOTIFY_ADD,
        election: mockElectionId,
        lao: mockLaoId,
        opened_at: TIMESTAMP,
      } as MessageDataProperties<OpenElection>);

      expect(msg.object).toEqual(ObjectType.ELECTION);
      expect(msg.action).toEqual(ActionType.OPEN);
    });
  });
});
