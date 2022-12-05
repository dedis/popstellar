import 'jest-extended';
import '__tests__/utils/matchers';

import { configureTestFeatures, mockLaoId, mockLaoName } from '__tests__/utils';
import { ActionType, ObjectType } from 'core/network/jsonrpc/messages';
import { Hash, ProtocolError, Timestamp } from 'core/objects';
import { MessageDataProperties } from 'core/types';
import { mockElectionOpened, mockElectionResultHash } from 'features/evoting/__tests__/utils';
import { Election } from 'features/evoting/objects';

import { EndElection } from '../EndElection';

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
const sampleEndElection: Partial<EndElection> = {
  object: ObjectType.ELECTION,
  action: ActionType.END,
  election: electionId,
  lao: mockLaoId,
  created_at: TIMESTAMP,
  registered_votes: mockElectionResultHash,
};

const endElectionJson: string = `{
  "object": "${ObjectType.ELECTION}",
  "action": "${ActionType.END}",
  "election": "${electionId}",
  "lao": "${mockLaoId}",
  "created_at": ${TIMESTAMP},
  "registered_votes": "${mockElectionResultHash}"
}`;

// endregion

beforeAll(() => {
  configureTestFeatures();
});

describe('EndElection', () => {
  it('should be created correctly from Json', () => {
    expect(new EndElection(sampleEndElection as MessageDataProperties<EndElection>)).toBeJsonEqual(
      sampleEndElection,
    );
    const temp = {
      object: ObjectType.ELECTION,
      action: ActionType.END,
      election: electionId,
      lao: mockLaoId,
      created_at: TIMESTAMP,
      registered_votes: mockElectionResultHash,
    };
    expect(new EndElection(temp)).toBeJsonEqual(temp);
  });

  it('should be parsed correctly from Json', () => {
    const obj = JSON.parse(endElectionJson);
    expect(EndElection.fromJson(obj)).toBeJsonEqual(sampleEndElection);
  });

  it('fromJson should throw an error if the Json has incorrect action', () => {
    const obj = {
      object: ObjectType.ELECTION,
      action: ActionType.NOTIFY_ADD,
      election: electionId.toString(),
      lao: mockLaoId.toString(),
      created_at: TIMESTAMP.valueOf(),
      registered_votes: mockElectionResultHash,
    };
    const createFromJson = () => EndElection.fromJson(obj);
    expect(createFromJson).toThrow(ProtocolError);
  });

  it('fromJson should throw an error if the Json has incorrect object', () => {
    const obj = {
      object: ObjectType.CHIRP,
      action: ActionType.OPEN,
      election: electionId.toString(),
      lao: mockLaoId.toString(),
      created_at: TIMESTAMP.valueOf(),
      registered_votes: mockElectionResultHash,
    };
    const createFromJson = () => EndElection.fromJson(obj);
    expect(createFromJson).toThrow(ProtocolError);
  });

  describe('constructor', () => {
    it('should throw an error if election is undefined', () => {
      const createWrongObj = () =>
        new EndElection({
          lao: mockLaoId,
          election: undefined as unknown as Hash,
          created_at: TIMESTAMP,
          registered_votes: mockElectionResultHash,
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if lao is undefined', () => {
      const createWrongObj = () =>
        new EndElection({
          lao: undefined as unknown as Hash,
          election: electionId,
          created_at: TIMESTAMP,
          registered_votes: mockElectionResultHash,
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if created_at is undefined', () => {
      const createWrongObj = () =>
        new EndElection({
          election: electionId,
          lao: mockLaoId,
          created_at: undefined as unknown as Timestamp,
          registered_votes: mockElectionResultHash,
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if registered_votes is undefined', () => {
      const createWrongObj = () =>
        new EndElection({
          election: electionId,
          lao: mockLaoId,
          created_at: TIMESTAMP,
          registered_votes: undefined as unknown as Hash,
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should ignore passed object and action parameters', () => {
      const msg = new EndElection({
        object: ObjectType.CHIRP,
        action: ActionType.NOTIFY_ADD,
        election: electionId,
        lao: mockLaoId,
        created_at: TIMESTAMP,
        registered_votes: mockElectionResultHash,
      } as MessageDataProperties<EndElection>);

      expect(msg.object).toEqual(ObjectType.ELECTION);
      expect(msg.action).toEqual(ActionType.END);
    });
  });

  describe('computeRegisteredVotesHash', () => {
    it('returns the correct result', () => {
      const fn = () => EndElection.computeRegisteredVotesHash(mockElectionOpened);
      expect(fn).not.toThrow();
      expect(fn().valueOf()).toEqual('mXvyOv7DcO_0Cl49W1EmUNs09HgGNASRCVGsZpEGsWA=');
    });

    it('does not mutate the election', () => {
      const frozenElection = Object.freeze({ ...mockElectionOpened });
      Object.freeze(frozenElection.registeredVotes);

      expect(() =>
        EndElection.computeRegisteredVotesHash(frozenElection as Election),
      ).not.toThrow();
    });
  });
});
