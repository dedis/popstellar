import 'jest-extended';
import '__tests__/utils/matchers';
import {
  mockLao,
  mockLaoId,
  mockLaoIdHash,
  mockLaoName,
  configureTestFeatures,
} from '__tests__/utils';

import { EventTags, Hash, Timestamp, ProtocolError } from 'core/objects';
import { ActionType, ObjectType } from 'core/network/jsonrpc/messages';
import { OpenedLaoStore } from 'features/lao/store';

import { MessageDataProperties } from 'core/types';
import STRINGS from 'resources/strings';
import { Election, Question, Vote } from '../../../objects';
import { CastVote } from '../CastVote';

const TIMESTAMP = new Timestamp(1609455600); // 1st january 2021
const VERSION = STRINGS.election_version_identifier;
const CLOSE_TIMESTAMP = new Timestamp(1609542000); // 2nd january 2021
const TIMESTAMP_BEFORE = new Timestamp(1609445600);

let electionId: Hash;
let election: Election;
let mockVoteObject1: Vote;
let mockVoteObject2: Vote;

// As discussed on slack, in these tests we should assume that the input to the messages is
// just a Partial<> and not a MessageDataProperties<>
// as this will catch more issues at runtime. (Defensive programming)
let sampleCastVote: Partial<CastVote>;

let CastVoteJson: string;

const initializeData = () => {
  electionId = Hash.fromStringArray('Election', mockLaoId, TIMESTAMP.toString(), mockLaoName);

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

  election = new Election({
    id: electionId,
    lao: mockLaoIdHash,
    name: 'An election',
    version: VERSION,
    createdAt: TIMESTAMP,
    start: TIMESTAMP_BEFORE,
    end: CLOSE_TIMESTAMP,
    questions: [mockQuestionObject1, mockQuestionObject2],
    registeredVotes: [],
    questionResult: [],
  });

  const mockVoteVotes1 = new Set([0]);
  const mockVoteVotes2 = new Set([1, 0]);

  const mockVoteId1 = CastVote.generateVoteId(election, 0, mockVoteVotes1);
  const mockVoteId2 = CastVote.generateVoteId(election, 1, mockVoteVotes2);

  mockVoteObject1 = {
    id: mockVoteId1.toString(),
    question: mockQuestionId1.valueOf(),
    vote: [0],
  };

  mockVoteObject2 = {
    id: mockVoteId2.toString(),
    question: mockQuestionId2.valueOf(),
    vote: [0],
  };

  const mockVotes = [mockVoteObject1];

  sampleCastVote = {
    object: ObjectType.ELECTION,
    action: ActionType.CAST_VOTE,
    lao: mockLaoIdHash,
    election: electionId,
    created_at: TIMESTAMP,
    votes: mockVotes,
  };

  CastVoteJson = `{
    "object": "${ObjectType.ELECTION}",
    "action": "${ActionType.CAST_VOTE}",
    "lao": "${mockLaoIdHash}",
    "election": "${electionId}",
    "created_at": ${TIMESTAMP},
    "votes": ${JSON.stringify(mockVotes)}
  }`;
};

beforeAll(() => {
  configureTestFeatures();
  initializeData();
  OpenedLaoStore.store(mockLao);
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
      created_at: parseInt(TIMESTAMP.toString(), 10),
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
      created_at: parseInt(TIMESTAMP.toString(), 10),
      votes: [mockVoteObject1, mockVoteObject2],
    };
    const createFromJson = () => CastVote.fromJson(obj);
    expect(createFromJson).toThrow(ProtocolError);
  });

  describe('constructor', () => {
    it('should throw an error if election is undefined', () => {
      const createWrongObj = () =>
        // @ts-ignore. Here was pass data to the constructor that is missing a field even though ts would require it
        new CastVote({
          lao: mockLaoIdHash,
          created_at: TIMESTAMP,
          votes: [mockVoteObject1, mockVoteObject2],
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if lao is undefined', () => {
      const createWrongObj = () =>
        // @ts-ignore. Here was pass data to the constructor that is missing a field even though ts would require it
        new CastVote({
          election: electionId,
          created_at: TIMESTAMP,
          votes: [mockVoteObject1, mockVoteObject2],
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if created_at is undefined', () => {
      const createWrongObj = () =>
        // @ts-ignore. Here was pass data to the constructor that is missing a field even though ts would require it
        new CastVote({
          lao: mockLaoIdHash,
          election: electionId,
          votes: [mockVoteObject1, mockVoteObject2],
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if questions is undefined', () => {
      const createWrongObj = () =>
        // @ts-ignore. Here was pass data to the constructor that is missing a field even though ts would require it
        new CastVote({
          lao: mockLaoIdHash,
          election: electionId,
          created_at: TIMESTAMP,
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should ignore passed object and action parameters', () => {
      const msg = new CastVote({
        // @ts-ignore. Here we pass additional fields to the constructor that should not be set
        object: ObjectType.CHIRP,
        action: ActionType.NOTIFY_ADD,
        lao: mockLaoIdHash,
        election: electionId,
        created_at: TIMESTAMP,
        votes: [mockVoteObject1, mockVoteObject2],
      });
      expect(msg.object).toEqual(ObjectType.ELECTION);
      expect(msg.action).toEqual(ActionType.CAST_VOTE);
    });
  });
});
