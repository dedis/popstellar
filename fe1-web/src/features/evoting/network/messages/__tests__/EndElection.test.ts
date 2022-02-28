import 'jest-extended';
import '__tests__/utils/matchers';
import {
  mockLao,
  mockLaoId,
  mockLaoIdHash,
  mockLaoName,
  configureTestFeatures,
} from '__tests__/utils';

import { Hash, Timestamp, ProtocolError, EventTags } from 'core/objects';
import { ActionType, ObjectType } from 'core/network/jsonrpc/messages';
import { OpenedLaoStore } from 'features/lao/store';

import { MessageDataProperties } from 'core/types';
import {
  Election,
  ElectionStatus,
  Question,
  QuestionResult,
  RegisteredVote,
} from 'features/evoting/objects';
import STRINGS from 'resources/strings';
import { EndElection } from '../EndElection';

const TIMESTAMP = new Timestamp(1609455600); // 1st january 2021
const CLOSE_TIMESTAMP = new Timestamp(1609542000); // 2nd january 2021

const mockElectionId = Hash.fromStringArray(
  'Election',
  mockLaoId,
  TIMESTAMP.toString(),
  mockLaoName,
);

const mockQuestion = 'Mock Question 1';
const mockQuestionId = Hash.fromStringArray(
  EventTags.QUESTION,
  mockElectionId.toString(),
  mockQuestion,
);
const mockBallotOptions = ['Ballot Option 1', 'Ballot Option 2'];

const question: Question = {
  id: mockQuestionId.toString(),
  question: mockQuestion,
  voting_method: STRINGS.election_method_Plurality,
  ballot_options: mockBallotOptions,
  write_in: false,
};

const election = new Election({
  lao: mockLaoIdHash,
  id: mockElectionId,
  name: 'An election',
  version: STRINGS.election_version_identifier,
  createdAt: TIMESTAMP,
  start: TIMESTAMP,
  end: CLOSE_TIMESTAMP,
  questions: [question],
  electionStatus: ElectionStatus.NOT_STARTED,
  registeredVotes: [],
});

const mockVoteId = 'x';
const mockElectionResultHash = Hash.fromStringArray(mockVoteId);

let electionId: Hash;

// As discussed on slack, in these tests we should assume that the input to the messages is
// just a Partial<> and not a MessageDataProperties<>
// as this will catch more issues at runtime. (Defensive programming)
let sampleEndElection: Partial<EndElection>;

let endElectionJson: string;

const initializeData = () => {
  electionId = Hash.fromStringArray('Election', mockLaoId, TIMESTAMP.toString(), mockLaoName);

  sampleEndElection = {
    object: ObjectType.ELECTION,
    action: ActionType.END,
    election: electionId,
    lao: mockLaoIdHash,
    created_at: TIMESTAMP,
    registered_votes: mockElectionResultHash,
  };

  endElectionJson = `{
    "object": "${ObjectType.ELECTION}",
    "action": "${ActionType.END}",
    "election": "${electionId}",
    "lao": "${mockLaoIdHash}",
    "created_at": ${TIMESTAMP},
    "registered_votes": "${mockElectionResultHash}"
  }`;
};

beforeAll(() => {
  configureTestFeatures();
  initializeData();
  OpenedLaoStore.store(mockLao);
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
      lao: mockLaoIdHash,
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
      lao: mockLaoIdHash.toString(),
      created_at: parseInt(TIMESTAMP.toString(), 10),
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
      lao: mockLaoIdHash.toString(),
      created_at: parseInt(TIMESTAMP.toString(), 10),
      registered_votes: mockElectionResultHash,
    };
    const createFromJson = () => EndElection.fromJson(obj);
    expect(createFromJson).toThrow(ProtocolError);
  });

  describe('constructor', () => {
    it('should throw an error if election is undefined', () => {
      const createWrongObj = () =>
        // @ts-ignore. Here was pass data to the constructor that is missing a field even though ts would require it
        new EndElection({
          lao: mockLaoIdHash,
          created_at: TIMESTAMP,
          registered_votes: mockElectionResultHash,
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if lao is undefined', () => {
      const createWrongObj = () =>
        // @ts-ignore. Here was pass data to the constructor that is missing a field even though ts would require it
        new EndElection({
          election: electionId,
          created_at: TIMESTAMP,
          registered_votes: mockElectionResultHash,
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if created_at is undefined', () => {
      const createWrongObj = () =>
        // @ts-ignore. Here was pass data to the constructor that is missing a field even though ts would require it
        new EndElection({
          election: electionId,
          lao: mockLaoIdHash,
          registered_votes: mockElectionResultHash,
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if registered_votes is undefined', () => {
      const createWrongObj = () =>
        // @ts-ignore. Here was pass data to the constructor that is missing a field even though ts would require it
        new EndElection({
          election: electionId,
          lao: mockLaoIdHash,
          created_at: TIMESTAMP,
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should ignore passed object and action parameters', () => {
      const msg = new EndElection({
        // @ts-ignore. Here we pass additional fields to the constructor that should not be set
        object: ObjectType.CHIRP,
        action: ActionType.NOTIFY_ADD,
        election: electionId,
        lao: mockLaoIdHash,
        created_at: TIMESTAMP,
        registered_votes: mockElectionResultHash,
      });

      expect(msg.object).toEqual(ObjectType.ELECTION);
      expect(msg.action).toEqual(ActionType.END);
    });
  });

  describe('computeRegisteredVotesHash', () => {
    // It seems a bit silly to test this function apart from it not returning an error as
    // we would simply write the same code here again?
    const fn = () => EndElection.computeRegisteredVotesHash(election);
    expect(fn).not.toThrow();
  });
});
