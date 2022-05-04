import 'jest-extended';
import '__tests__/utils/matchers';

import { configureTestFeatures, mockLaoIdHash } from '__tests__/utils';
import { ActionType, ObjectType } from 'core/network/jsonrpc/messages';
import { Hash, ProtocolError, Timestamp } from 'core/objects';
import { MessageDataProperties } from 'core/types';
import {
  mockElectionId,
  mockElectionOpened,
  mockSecretBallotElectionNotStarted,
  mockVote1,
  mockVote2,
  mockVotes,
} from 'features/evoting/__tests__/utils';
import { ElectionKeyPair } from 'features/evoting/objects/ElectionKeyPair';

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

  describe('validateVotes', () => {
    it('returns true if all fields are defined', () => {
      expect(() => CastVote.validateVotes([])).not.toThrow();
      expect(() =>
        CastVote.validateVotes([{ id: 'someId', question: 'q', vote: [0] }]),
      ).not.toThrow();
    });

    it('returns false if some fields are undefined', () => {
      expect(() =>
        CastVote.validateVotes([{ id: 'someId', question: 'q', vote: undefined as any }]),
      ).toThrow(ProtocolError);

      expect(() =>
        CastVote.validateVotes([{ id: 'someId', question: undefined as any, vote: [0] }]),
      ).toThrow(ProtocolError);

      expect(() =>
        CastVote.validateVotes([{ id: undefined as any, question: 'q', vote: [0] }]),
      ).toThrow(ProtocolError);
    });
  });

  describe('selectedBallotsToVotes', () => {
    const q1SelectedOptions = new Set([0]);
    const q2SelectedOptions = new Set([1]);

    expect(
      CastVote.selectedBallotsToVotes(mockElectionOpened, {
        0: q1SelectedOptions,
        1: q2SelectedOptions,
      }),
    ).toEqual([
      {
        id: CastVote.computeVoteId(mockElectionOpened, 0, q1SelectedOptions).valueOf(),
        question: mockElectionOpened.questions[0].id,
        vote: [...q1SelectedOptions],
      },
      {
        id: CastVote.computeVoteId(mockElectionOpened, 1, q2SelectedOptions).valueOf(),
        question: mockElectionOpened.questions[1].id,
        vote: [...q2SelectedOptions],
      },
    ] as Vote[]);
  });

  describe('selectedBallotsToEncryptedVotes', () => {
    const q1SelectedOptions = new Set([0]);
    const q2SelectedOptions = new Set([1]);

    const keyPair = ElectionKeyPair.generate();

    const encryptedVotes = CastVote.selectedBallotsToEncryptedVotes(
      mockSecretBallotElectionNotStarted,
      keyPair.publicKey,
      {
        0: q1SelectedOptions,
        1: q2SelectedOptions,
      },
    );

    expect(encryptedVotes.length).toBe(2);
    expect(encryptedVotes[0]).toHaveProperty(
      'id',
      CastVote.computeVoteId(mockElectionOpened, 0, q1SelectedOptions).valueOf(),
    );
    expect(encryptedVotes[0]).toHaveProperty('question', mockElectionOpened.questions[0].id);
    expect(encryptedVotes[0].vote.length).toBe(1);
    expect(keyPair.privateKey.decrypt(encryptedVotes[0].vote[0]).readIntBE(0, 2)).toEqual(0);

    expect(encryptedVotes[1]).toHaveProperty(
      'id',
      CastVote.computeVoteId(mockElectionOpened, 1, q2SelectedOptions).valueOf(),
    );
    expect(encryptedVotes[1]).toHaveProperty('question', mockElectionOpened.questions[1].id);
    expect(encryptedVotes[1].vote.length).toBe(1);
    expect(keyPair.privateKey.decrypt(encryptedVotes[1].vote[0]).readIntBE(0, 2)).toEqual(1);
  });

  describe('computeVoteId', () => {});
});
