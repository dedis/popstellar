import 'jest-extended';
import '__tests__/utils/matchers';

import { configureTestFeatures, mockLaoIdHash } from '__tests__/utils';
import { ActionType, ObjectType } from 'core/network/jsonrpc/messages';
import { Hash, ProtocolError, Timestamp } from 'core/objects';
import { MessageDataProperties } from 'core/types';
import {
  mockElectionId,
  mockVote1,
  mockVote2,
  mockVotes,
} from 'features/evoting/objects/__tests__/utils';

import { Vote } from '../../../objects';
import { CastVote } from '../CastVote';

// region test data initialization

const TIMESTAMP = new Timestamp(1609455600); // 1st january 2021

// In these tests, we should assume that the input to the messages is
// just a Partial<> and not a MessageDataProperties<>
// as this will catch more issues at runtime. (Defensive programming)
const sampleCastVote: Partial<CastVote> = {
  object: ObjectType.ELECTION,
  action: ActionType.CAST_VOTE,
  lao: mockLaoIdHash,
  election: mockElectionId,
  created_at: TIMESTAMP,
  votes: mockVotes,
};

const CastVoteJson: string = `{
  "object": "${ObjectType.ELECTION}",
  "action": "${ActionType.CAST_VOTE}",
  "lao": "${mockLaoIdHash}",
  "election": "${mockElectionId}",
  "created_at": ${TIMESTAMP},
  "votes": ${JSON.stringify(mockVotes)}
}`;

// endregion

beforeAll(() => {
  configureTestFeatures();
});

describe('CastVote', () => {
  it('should be created correctly from Json', () => {
    expect(new CastVote(sampleCastVote as MessageDataProperties<CastVote>)).toBeJsonEqual(
      sampleCastVote,
    );

    const temp = {
      object: ObjectType.ELECTION,
      action: ActionType.CAST_VOTE,
      lao: mockLaoIdHash,
      election: mockElectionId,
      created_at: TIMESTAMP,
      votes: [mockVote1, mockVote2],
    };
    expect(new CastVote(temp)).toBeJsonEqual(temp);
  });

  it('should be parsed correctly from Json', () => {
    const obj = JSON.parse(CastVoteJson);
    expect(CastVote.fromJson(obj)).toBeJsonEqual(sampleCastVote);
  });

  it('fromJson should throw an error if the Json has incorrect action', () => {
    const obj = {
      object: ObjectType.ELECTION,
      action: ActionType.NOTIFY_ADD,
      lao: mockLaoIdHash.toString(),
      election: mockElectionId.toString(),
      created_at: TIMESTAMP.valueOf(),
      votes: [mockVote1, mockVote2],
    };
    const createFromJson = () => CastVote.fromJson(obj);
    expect(createFromJson).toThrow(ProtocolError);
  });

  it('fromJson should throw an error if the Json has incorrect object', () => {
    const obj = {
      object: ObjectType.CHIRP,
      action: ActionType.CAST_VOTE,
      lao: mockLaoIdHash.toString(),
      election: mockElectionId.toString(),
      created_at: TIMESTAMP.valueOf(),
      votes: [mockVote1, mockVote2],
    };
    const createFromJson = () => CastVote.fromJson(obj);
    expect(createFromJson).toThrow(ProtocolError);
  });

  describe('constructor', () => {
    it('should throw an error if election is undefined', () => {
      const createWrongObj = () =>
        new CastVote({
          lao: mockLaoIdHash,
          election: undefined as unknown as Hash,
          created_at: TIMESTAMP,
          votes: [mockVote1, mockVote2],
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if lao is undefined', () => {
      const createWrongObj = () =>
        new CastVote({
          lao: undefined as unknown as Hash,
          election: mockElectionId,
          created_at: TIMESTAMP,
          votes: [mockVote1, mockVote2],
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if created_at is undefined', () => {
      const createWrongObj = () =>
        new CastVote({
          lao: mockLaoIdHash,
          election: mockElectionId,
          votes: [mockVote1, mockVote2],
          created_at: undefined as unknown as Timestamp,
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if votes is undefined', () => {
      const createWrongObj = () =>
        new CastVote({
          lao: mockLaoIdHash,
          election: mockElectionId,
          created_at: TIMESTAMP,
          votes: undefined as unknown as Vote[],
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should ignore passed object and action parameters', () => {
      const msg = new CastVote({
        object: ObjectType.CHIRP,
        action: ActionType.NOTIFY_ADD,
        lao: mockLaoIdHash,
        election: mockElectionId,
        created_at: TIMESTAMP,
        votes: [mockVote1, mockVote2],
      } as MessageDataProperties<CastVote>);
      expect(msg.object).toEqual(ObjectType.ELECTION);
      expect(msg.action).toEqual(ActionType.CAST_VOTE);
    });
  });
});
