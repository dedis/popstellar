import 'jest-extended';
import '__tests__/utils/matchers';
import STRINGS from 'res/strings';
import { LaoEventType } from '../LaoEvent';
import {
  Election, ElectionState, ElectionStatus, Question, RegisteredVote, Vote,
} from '../Election';
import { Timestamp } from '../Timestamp';
import { Hash } from '../Hash';

const question1: Question = {
  id: 'q1',
  question: 'Question1',
  voting_method: STRINGS.election_method_Plurality,
  ballot_options: ['Answer1.1', 'Answer1.2', 'Answer 1.3'],
  write_in: false,
};

const question2: Question = {
  id: 'q2',
  question: 'Question2',
  voting_method: STRINGS.election_method_Plurality,
  ballot_options: ['Answer2.1', 'Answer2.2'],
  write_in: false,
};

const vote1: Vote = {
  id: 'v1',
  question: 'q1',
};

const registeredVotes: RegisteredVote = {
  createdAt: 1520255700,
  sender: 'Sender1',
  votes: [vote1],
  messageId: 'messageId1',
};

const electionState: ElectionState = {
  id: 'electionId',
  eventType: LaoEventType.ELECTION,
  lao: 'MyLao',
  name: 'MyElection',
  version: 'version',
  created_at: 1520255600,
  start: 1520255600,
  end: 1520275600,
  questions: [question1, question2],
  registered_votes: [registeredVotes],
};

const TIMESTAMP_PAST1 = new Timestamp(1520255600);
const TIMESTAMP_PAST2 = new Timestamp(1520275600);
const TIMESTAMP_FUTURE1 = new Timestamp(1620655600);
const TIMESTAMP_FUTURE2 = new Timestamp(1620755600);
const ELECTION_ID = new Hash('electionId');
const LAO_ID = new Hash('MyLao');
const NAME = 'MyElection';
const VERSION = 'version';
const QUESTIONS = [question1, question2];
const REGISTERED_VOTES = [registeredVotes];

beforeAll(() => {
  jest.useFakeTimers('modern');
  jest.setSystemTime(new Date(1620255600000)); // 5 May 2021
});

describe('Election object', () => {
  it('does a state round trip correctly', () => {
    const election = Election.fromState(electionState);
    const expectedState = {
      id: 'electionId',
      eventType: LaoEventType.ELECTION,
      lao: 'MyLao',
      name: 'MyElection',
      version: 'version',
      created_at: 1520255600,
      start: 1520255600,
      end: 1520275600,
      questions: [question1, question2],
      registered_votes: [registeredVotes],
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
        lao: LAO_ID,
        name: NAME,
        version: VERSION,
        created_at: TIMESTAMP_PAST1,
        start: TIMESTAMP_PAST1,
        end: TIMESTAMP_PAST2,
        questions: QUESTIONS,
        registered_votes: REGISTERED_VOTES,
      });
      expect(createWrongElection).toThrow(Error);
    });

    it('throws an error when lao is undefined', () => {
      const createWrongElection = () => new Election({
        id: ELECTION_ID,
        name: NAME,
        version: VERSION,
        created_at: TIMESTAMP_PAST1,
        start: TIMESTAMP_PAST1,
        end: TIMESTAMP_PAST2,
        questions: QUESTIONS,
        registered_votes: REGISTERED_VOTES,
      });
      expect(createWrongElection).toThrow(Error);
    });

    it('throws an error when name is undefined', () => {
      const createWrongElection = () => new Election({
        id: ELECTION_ID,
        lao: LAO_ID,
        version: VERSION,
        created_at: TIMESTAMP_PAST1,
        start: TIMESTAMP_PAST1,
        end: TIMESTAMP_PAST2,
        questions: QUESTIONS,
        registered_votes: REGISTERED_VOTES,
      });
      expect(createWrongElection).toThrow(Error);
    });

    it('throws an error when version is undefined', () => {
      const createWrongElection = () => new Election({
        id: ELECTION_ID,
        lao: LAO_ID,
        name: NAME,
        created_at: TIMESTAMP_PAST1,
        start: TIMESTAMP_PAST1,
        end: TIMESTAMP_PAST2,
        questions: QUESTIONS,
        registered_votes: REGISTERED_VOTES,
      });
      expect(createWrongElection).toThrow(Error);
    });

    it('throws an error when created_at is undefined', () => {
      const createWrongElection = () => new Election({
        id: ELECTION_ID,
        lao: LAO_ID,
        name: NAME,
        version: VERSION,
        start: TIMESTAMP_PAST1,
        end: TIMESTAMP_PAST2,
        questions: QUESTIONS,
        registered_votes: REGISTERED_VOTES,
      });
      expect(createWrongElection).toThrow(Error);
    });

    it('throws an error when start is undefined', () => {
      const createWrongElection = () => new Election({
        id: ELECTION_ID,
        lao: LAO_ID,
        name: NAME,
        version: VERSION,
        created_at: TIMESTAMP_PAST1,
        end: TIMESTAMP_PAST2,
        questions: QUESTIONS,
        registered_votes: REGISTERED_VOTES,
      });
      expect(createWrongElection).toThrow(Error);
    });

    it('throws an error when end is undefined', () => {
      const createWrongElection = () => new Election({
        id: ELECTION_ID,
        lao: LAO_ID,
        name: NAME,
        version: VERSION,
        created_at: TIMESTAMP_PAST1,
        start: TIMESTAMP_PAST1,
        questions: QUESTIONS,
        registered_votes: REGISTERED_VOTES,
      });
      expect(createWrongElection).toThrow(Error);
    });

    it('throws an error when questions is undefined', () => {
      const createWrongElection = () => new Election({
        id: ELECTION_ID,
        lao: LAO_ID,
        name: NAME,
        version: VERSION,
        created_at: TIMESTAMP_PAST1,
        start: TIMESTAMP_PAST1,
        end: TIMESTAMP_PAST2,
        registered_votes: REGISTERED_VOTES,
      });
      expect(createWrongElection).toThrow(Error);
    });

    it('creates an election when registered_votes is undefined', () => {
      const election = new Election({
        id: ELECTION_ID,
        lao: LAO_ID,
        name: NAME,
        version: VERSION,
        created_at: TIMESTAMP_PAST1,
        start: TIMESTAMP_PAST1,
        end: TIMESTAMP_PAST2,
        questions: QUESTIONS,
      });
      const expected = new Election({
        id: ELECTION_ID,
        lao: LAO_ID,
        name: NAME,
        version: VERSION,
        created_at: TIMESTAMP_PAST1,
        start: TIMESTAMP_PAST1,
        end: TIMESTAMP_PAST2,
        questions: QUESTIONS,
        registered_votes: [],
      });
      expect(election).toStrictEqual(expected);
    });

    it('sets correct electionStatus when running', () => {
      const election = new Election({
        id: ELECTION_ID,
        lao: LAO_ID,
        name: NAME,
        version: VERSION,
        created_at: TIMESTAMP_PAST2,
        start: TIMESTAMP_PAST2,
        end: TIMESTAMP_FUTURE1,
        questions: QUESTIONS,
      });
      const expected = new Election({
        id: ELECTION_ID,
        lao: LAO_ID,
        name: NAME,
        version: VERSION,
        created_at: TIMESTAMP_PAST2,
        start: TIMESTAMP_PAST2,
        end: TIMESTAMP_FUTURE1,
        questions: QUESTIONS,
        registered_votes: [],
        electionStatus: ElectionStatus.RUNNING,
      });
      expect(election).toStrictEqual(expected);
    });

    it('sets correct electionStatus when not started', () => {
      const election = new Election({
        id: ELECTION_ID,
        lao: LAO_ID,
        name: NAME,
        version: VERSION,
        created_at: TIMESTAMP_FUTURE1,
        start: TIMESTAMP_FUTURE1,
        end: TIMESTAMP_FUTURE2,
        questions: QUESTIONS,
      });
      const expected = new Election({
        id: ELECTION_ID,
        lao: LAO_ID,
        name: NAME,
        version: VERSION,
        created_at: TIMESTAMP_FUTURE1,
        start: TIMESTAMP_FUTURE1,
        end: TIMESTAMP_FUTURE2,
        questions: QUESTIONS,
        registered_votes: [],
        electionStatus: ElectionStatus.NOTSTARTED,
      });
      expect(election).toStrictEqual(expected);
    });
  });
});
