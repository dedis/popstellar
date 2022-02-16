import 'jest-extended';

import '__tests__/utils/matchers';
import STRINGS from 'res/strings';
import { mockLaoIdHash, mockLaoName } from '__tests__/utils/TestUtils';
import { LaoEventType } from 'model/objects/LaoEvent';
import { Timestamp } from 'model/objects/Timestamp';
import { Hash } from 'model/objects/Hash';

import {
  Election, ElectionState, ElectionStatus, Question, RegisteredVote, Vote,
} from '../Election';

let question1: Question;
let question2: Question;
let vote1: Vote;
let registeredVotes: RegisteredVote;
let electionState: ElectionState;
let QUESTIONS: Question[];
let REGISTERED_VOTES: RegisteredVote[];

const initializeData = () => {
  question1 = {
    id: 'q1',
    question: 'Question1',
    voting_method: STRINGS.election_method_Plurality,
    ballot_options: ['Answer1.1', 'Answer1.2', 'Answer 1.3'],
    write_in: false,
  };

  question2 = {
    id: 'q2',
    question: 'Question2',
    voting_method: STRINGS.election_method_Plurality,
    ballot_options: ['Answer2.1', 'Answer2.2'],
    write_in: false,
  };

  vote1 = {
    id: 'v1',
    question: 'q1',
  };

  registeredVotes = {
    createdAt: 1520255700,
    sender: 'Sender1',
    votes: [vote1],
    messageId: 'messageId1',
  };

  electionState = {
    id: 'electionId',
    eventType: LaoEventType.ELECTION,
    lao: 'MyLao',
    name: 'MyElection',
    version: 'version',
    createdAt: 1520255600,
    start: 1520255600,
    end: 1520275600,
    questions: [question1, question2],
    registeredVotes: [registeredVotes],
  };

  QUESTIONS = [question1, question2];
  REGISTERED_VOTES = [registeredVotes];
};

const TIMESTAMP_PAST1 = new Timestamp(1520255600);
const TIMESTAMP_PAST2 = new Timestamp(1520275600);
const TIMESTAMP_FUTURE1 = new Timestamp(1620655600);
const TIMESTAMP_FUTURE2 = new Timestamp(1620755600);
const ELECTION_ID = new Hash('electionId');
const NAME = 'MyElection';
const VERSION = 'version';

beforeAll(() => {
  jest.useFakeTimers('modern');
  jest.setSystemTime(new Date(1620255600000)); // 5 May 2021
});

beforeEach(() => {
  initializeData();
});

describe('Election object', () => {
  it('does a state round trip correctly', () => {
    const election = Election.fromState(electionState);
    const expectedState = {
      id: ELECTION_ID.valueOf(),
      eventType: LaoEventType.ELECTION,
      lao: mockLaoName,
      name: NAME,
      version: VERSION,
      createdAt: TIMESTAMP_PAST1.valueOf(),
      start: TIMESTAMP_PAST1.valueOf(),
      end: TIMESTAMP_PAST2.valueOf(),
      questions: [question1, question2],
      registeredVotes: [registeredVotes],
      electionStatus: ElectionStatus.FINISHED,
    };
    expect(election.toState()).toStrictEqual(expectedState);
  });

  describe('constructor', () => {
    it('throws an error when object is undefined', () => {
      const partial = undefined as unknown as Partial<Election>;
      const createWrongElection = () => new Election(partial);
      expect(createWrongElection).toThrow(Error);
    });

    it('throws an error when object is null', () => {
      const partial = null as unknown as Partial<Election>;
      const createWrongElection = () => new Election(partial);
      expect(createWrongElection).toThrow(Error);
    });

    it('throws an error when id is undefined', () => {
      const createWrongElection = () => new Election({
        lao: mockLaoIdHash,
        name: NAME,
        version: VERSION,
        createdAt: TIMESTAMP_PAST1,
        start: TIMESTAMP_PAST1,
        end: TIMESTAMP_PAST2,
        questions: QUESTIONS,
        registeredVotes: REGISTERED_VOTES,
      });
      expect(createWrongElection).toThrow(Error);
    });

    it('throws an error when lao is undefined', () => {
      const createWrongElection = () => new Election({
        id: ELECTION_ID,
        name: NAME,
        version: VERSION,
        createdAt: TIMESTAMP_PAST1,
        start: TIMESTAMP_PAST1,
        end: TIMESTAMP_PAST2,
        questions: QUESTIONS,
        registeredVotes: REGISTERED_VOTES,
      });
      expect(createWrongElection).toThrow(Error);
    });

    it('throws an error when name is undefined', () => {
      const createWrongElection = () => new Election({
        id: ELECTION_ID,
        lao: mockLaoIdHash,
        version: VERSION,
        createdAt: TIMESTAMP_PAST1,
        start: TIMESTAMP_PAST1,
        end: TIMESTAMP_PAST2,
        questions: QUESTIONS,
        registeredVotes: REGISTERED_VOTES,
      });
      expect(createWrongElection).toThrow(Error);
    });

    it('throws an error when version is undefined', () => {
      const createWrongElection = () => new Election({
        id: ELECTION_ID,
        lao: mockLaoIdHash,
        name: NAME,
        createdAt: TIMESTAMP_PAST1,
        start: TIMESTAMP_PAST1,
        end: TIMESTAMP_PAST2,
        questions: QUESTIONS,
        registeredVotes: REGISTERED_VOTES,
      });
      expect(createWrongElection).toThrow(Error);
    });

    it('throws an error when created_at is undefined', () => {
      const createWrongElection = () => new Election({
        id: ELECTION_ID,
        lao: mockLaoIdHash,
        name: NAME,
        version: VERSION,
        start: TIMESTAMP_PAST1,
        end: TIMESTAMP_PAST2,
        questions: QUESTIONS,
        registeredVotes: REGISTERED_VOTES,
      });
      expect(createWrongElection).toThrow(Error);
    });

    it('throws an error when start is undefined', () => {
      const createWrongElection = () => new Election({
        id: ELECTION_ID,
        lao: mockLaoIdHash,
        name: NAME,
        version: VERSION,
        createdAt: TIMESTAMP_PAST1,
        end: TIMESTAMP_PAST2,
        questions: QUESTIONS,
        registeredVotes: REGISTERED_VOTES,
      });
      expect(createWrongElection).toThrow(Error);
    });

    it('throws an error when end is undefined', () => {
      const createWrongElection = () => new Election({
        id: ELECTION_ID,
        lao: mockLaoIdHash,
        name: NAME,
        version: VERSION,
        createdAt: TIMESTAMP_PAST1,
        start: TIMESTAMP_PAST1,
        questions: QUESTIONS,
        registeredVotes: REGISTERED_VOTES,
      });
      expect(createWrongElection).toThrow(Error);
    });

    it('throws an error when questions is undefined', () => {
      const createWrongElection = () => new Election({
        id: ELECTION_ID,
        lao: mockLaoIdHash,
        name: NAME,
        version: VERSION,
        createdAt: TIMESTAMP_PAST1,
        start: TIMESTAMP_PAST1,
        end: TIMESTAMP_PAST2,
        registeredVotes: REGISTERED_VOTES,
      });
      expect(createWrongElection).toThrow(Error);
    });

    it('creates an election when registered_votes is undefined', () => {
      const election = new Election({
        id: ELECTION_ID,
        lao: mockLaoIdHash,
        name: NAME,
        version: VERSION,
        createdAt: TIMESTAMP_PAST1,
        start: TIMESTAMP_PAST1,
        end: TIMESTAMP_PAST2,
        questions: QUESTIONS,
      });
      const expected = new Election({
        id: ELECTION_ID,
        lao: mockLaoIdHash,
        name: NAME,
        version: VERSION,
        createdAt: TIMESTAMP_PAST1,
        start: TIMESTAMP_PAST1,
        end: TIMESTAMP_PAST2,
        questions: QUESTIONS,
        registeredVotes: [],
      });
      expect(election).toStrictEqual(expected);
    });

    it('sets correct electionStatus when running', () => {
      const election = new Election({
        id: ELECTION_ID,
        lao: mockLaoIdHash,
        name: NAME,
        version: VERSION,
        createdAt: TIMESTAMP_PAST2,
        start: TIMESTAMP_PAST2,
        end: TIMESTAMP_FUTURE1,
        questions: QUESTIONS,
      });
      const expected = new Election({
        id: ELECTION_ID,
        lao: mockLaoIdHash,
        name: NAME,
        version: VERSION,
        createdAt: TIMESTAMP_PAST2,
        start: TIMESTAMP_PAST2,
        end: TIMESTAMP_FUTURE1,
        questions: QUESTIONS,
        registeredVotes: [],
        electionStatus: ElectionStatus.RUNNING,
      });
      expect(election).toStrictEqual(expected);
    });

    it('sets correct electionStatus when not started', () => {
      const election = new Election({
        id: ELECTION_ID,
        lao: mockLaoIdHash,
        name: NAME,
        version: VERSION,
        createdAt: TIMESTAMP_FUTURE1,
        start: TIMESTAMP_FUTURE1,
        end: TIMESTAMP_FUTURE2,
        questions: QUESTIONS,
      });
      const expected = new Election({
        id: ELECTION_ID,
        lao: mockLaoIdHash,
        name: NAME,
        version: VERSION,
        createdAt: TIMESTAMP_FUTURE1,
        start: TIMESTAMP_FUTURE1,
        end: TIMESTAMP_FUTURE2,
        questions: QUESTIONS,
        registeredVotes: [],
        electionStatus: ElectionStatus.NOT_STARTED,
      });
      expect(election).toStrictEqual(expected);
    });
  });
});

afterAll(() => {
  jest.useRealTimers();
});
