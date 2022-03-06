import 'jest-extended';
import '__tests__/utils/matchers';
import { mockLaoId, mockLaoIdHash, mockLaoName, configureTestFeatures } from '__tests__/utils';

import { EventTags, Hash, Timestamp, ProtocolError } from 'core/objects';
import { ActionType, ObjectType } from 'core/network/jsonrpc/messages';

import { MessageDataProperties } from 'core/types';
import STRINGS from 'resources/strings';
import { Election, ElectionStatus, Question, Vote } from '../../../objects';
import { CastVote } from '../CastVote';

// region test data initialization

const TIMESTAMP = new Timestamp(1609455600); // 1st january 2021
const VERSION = STRINGS.election_version_identifier;
const CLOSE_TIMESTAMP = new Timestamp(1609542000); // 2nd january 2021
const TIMESTAMP_BEFORE = new Timestamp(1609445600);

const electionId: Hash = Hash.fromStringArray(
  'Election',
  mockLaoId,
  TIMESTAMP.toString(),
  mockLaoName,
);

const mockQuestion1 = 'Mock Question 1';
const mockQuestion2 = 'Mock Question 2';
const mockQuestionId1 = Hash.fromStringArray(
  EventTags.QUESTION,
  electionId.toString(),
  mockQuestion1,
);

const mockQuestionId2 = Hash.fromStringArray(
  EventTags.QUESTION,
  electionId.toString(),
  mockQuestion2,
);

const mockBallotOptions = ['Ballot Option 1', 'Ballot Option 2'];

const mockQuestionObject1: Question = {
  id: mockQuestionId1.toString(),
  question: mockQuestion1,
  voting_method: STRINGS.election_method_Plurality,
  ballot_options: mockBallotOptions,
  write_in: false,
};

const mockQuestionObject2: Question = {
  id: mockQuestionId2.toString(),
  question: mockQuestion2,
  voting_method: STRINGS.election_method_Approval,
  ballot_options: mockBallotOptions,
  write_in: true,
};

const election: Election = new Election({
  id: electionId,
  lao: mockLaoIdHash,
  name: 'An election',
  version: VERSION,
  createdAt: TIMESTAMP,
  start: TIMESTAMP_BEFORE,
  end: CLOSE_TIMESTAMP,
  questions: [mockQuestionObject1, mockQuestionObject2],
  electionStatus: ElectionStatus.OPENED,
  registeredVotes: [],
  questionResult: [],
});

const mockVoteVotes1 = new Set([0]);
const mockVoteVotes2 = new Set([1, 0]);

const mockVoteId1 = CastVote.computeVoteId(election, 0, mockVoteVotes1);
const mockVoteId2 = CastVote.computeVoteId(election, 1, mockVoteVotes2);

const mockVoteObject1: Vote = {
  id: mockVoteId1.toString(),
  question: mockQuestionId1.valueOf(),
  vote: [0],
};

const mockVoteObject2: Vote = {
  id: mockVoteId2.toString(),
  question: mockQuestionId2.valueOf(),
  vote: [0],
};

const mockVotes = [mockVoteObject1];

// In these tests, we should assume that the input to the messages is
// just a Partial<> and not a MessageDataProperties<>
// as this will catch more issues at runtime. (Defensive programming)
const sampleCastVote: Partial<CastVote> = {
  object: ObjectType.ELECTION,
  action: ActionType.CAST_VOTE,
  lao: mockLaoIdHash,
  election: electionId,
  created_at: TIMESTAMP,
  votes: mockVotes,
};

const CastVoteJson: string = `{
  "object": "${ObjectType.ELECTION}",
  "action": "${ActionType.CAST_VOTE}",
  "lao": "${mockLaoIdHash}",
  "election": "${electionId}",
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
      election: electionId,
      created_at: TIMESTAMP,
      votes: [mockVoteObject1, mockVoteObject2],
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
      election: electionId.toString(),
      created_at: TIMESTAMP.valueOf(),
      votes: [mockVoteObject1, mockVoteObject2],
    };
    const createFromJson = () => CastVote.fromJson(obj);
    expect(createFromJson).toThrow(ProtocolError);
  });

  it('fromJson should throw an error if the Json has incorrect object', () => {
    const obj = {
      object: ObjectType.CHIRP,
      action: ActionType.CAST_VOTE,
      lao: mockLaoIdHash.toString(),
      election: electionId.toString(),
      created_at: TIMESTAMP.valueOf(),
      votes: [mockVoteObject1, mockVoteObject2],
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
          votes: [mockVoteObject1, mockVoteObject2],
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if lao is undefined', () => {
      const createWrongObj = () =>
        new CastVote({
          lao: undefined as unknown as Hash,
          election: electionId,
          created_at: TIMESTAMP,
          votes: [mockVoteObject1, mockVoteObject2],
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if created_at is undefined', () => {
      const createWrongObj = () =>
        new CastVote({
          lao: mockLaoIdHash,
          election: electionId,
          votes: [mockVoteObject1, mockVoteObject2],
          created_at: undefined as unknown as Timestamp,
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if votes is undefined', () => {
      const createWrongObj = () =>
        new CastVote({
          lao: mockLaoIdHash,
          election: electionId,
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
        election: electionId,
        created_at: TIMESTAMP,
        votes: [mockVoteObject1, mockVoteObject2],
      } as MessageDataProperties<CastVote>);
      expect(msg.object).toEqual(ObjectType.ELECTION);
      expect(msg.action).toEqual(ActionType.CAST_VOTE);
    });
  });
});
