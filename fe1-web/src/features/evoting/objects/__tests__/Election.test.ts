import 'jest-extended';
import '__tests__/utils/matchers';

import { mockLaoId, mockLaoName } from '__tests__/utils/TestUtils';
import { Hash, PublicKey, Timestamp } from 'core/objects';
import STRINGS from 'resources/strings';

import {
  Election,
  ElectionStatus,
  Question,
  RegisteredVote,
  Vote,
  ElectionVersion,
} from '../index';

const question1 = new Question({
  id: new Hash('q1'),
  question: 'Question1',
  voting_method: STRINGS.election_method_Plurality,
  ballot_options: ['Answer1.1', 'Answer1.2', 'Answer 1.3'],
  write_in: false,
});

const question2 = new Question({
  id: new Hash('q2'),
  question: 'Question2',
  voting_method: STRINGS.election_method_Plurality,
  ballot_options: ['Answer2.1', 'Answer2.2'],
  write_in: false,
});

const vote1 = new Vote({
  id: new Hash('v1'),
  question: new Hash('q1'),
  vote: 0,
});
const registeredVotes = new RegisteredVote({
  createdAt: new Timestamp(1520255700),
  sender: new PublicKey('Sender1'),
  votes: [vote1],
  messageId: new Hash('messageId1'),
});

const election = new Election({
  id: new Hash('electionId'),
  lao: new Hash('MyLao'),
  name: 'MyElection',
  version: ElectionVersion.OPEN_BALLOT,
  createdAt: new Timestamp(1520255600),
  start: new Timestamp(1520255600),
  end: new Timestamp(1520275600),
  questions: [question1, question2],
  electionStatus: ElectionStatus.TERMINATED,
  registeredVotes: [registeredVotes],
});

const QUESTIONS: Question[] = [question1, question2];
const REGISTERED_VOTES: RegisteredVote[] = [registeredVotes];

const TIMESTAMP_PAST1 = new Timestamp(1520255600);
const TIMESTAMP_PAST2 = new Timestamp(1520275600);
const ELECTION_ID = new Hash('electionId');
const NAME = 'MyElection';

describe('Election object', () => {
  it('does a state round trip correctly', () => {
    const e = Election.fromState(election.toState());
    const expectedState = {
      id: ELECTION_ID.toState(),
      lao: mockLaoName,
      name: NAME,
      version: ElectionVersion.OPEN_BALLOT,
      createdAt: TIMESTAMP_PAST1.toState(),
      start: TIMESTAMP_PAST1.toState(),
      end: TIMESTAMP_PAST2.toState(),
      questions: [question1.toState(), question2.toState()],
      registeredVotes: [registeredVotes.toState()],
      questionResult: undefined,
      electionStatus: ElectionStatus.TERMINATED,
    };
    expect(e.toState()).toStrictEqual(expectedState);
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
      const createWrongElection = () =>
        new Election({
          lao: mockLaoId,
          name: NAME,
          version: ElectionVersion.OPEN_BALLOT,
          createdAt: TIMESTAMP_PAST1,
          start: TIMESTAMP_PAST1,
          end: TIMESTAMP_PAST2,
          questions: QUESTIONS,
          registeredVotes: REGISTERED_VOTES,
          electionStatus: ElectionStatus.NOT_STARTED,
        });
      expect(createWrongElection).toThrow(Error);
    });

    it('throws an error when lao is undefined', () => {
      const createWrongElection = () =>
        new Election({
          id: ELECTION_ID,
          name: NAME,
          version: ElectionVersion.OPEN_BALLOT,
          createdAt: TIMESTAMP_PAST1,
          start: TIMESTAMP_PAST1,
          end: TIMESTAMP_PAST2,
          questions: QUESTIONS,
          registeredVotes: REGISTERED_VOTES,
          electionStatus: ElectionStatus.NOT_STARTED,
        });
      expect(createWrongElection).toThrow(Error);
    });

    it('throws an error when name is undefined', () => {
      const createWrongElection = () =>
        new Election({
          id: ELECTION_ID,
          lao: mockLaoId,
          version: ElectionVersion.OPEN_BALLOT,
          createdAt: TIMESTAMP_PAST1,
          start: TIMESTAMP_PAST1,
          end: TIMESTAMP_PAST2,
          questions: QUESTIONS,
          registeredVotes: REGISTERED_VOTES,
          electionStatus: ElectionStatus.NOT_STARTED,
        });
      expect(createWrongElection).toThrow(Error);
    });

    it('throws an error when version is undefined', () => {
      const createWrongElection = () =>
        new Election({
          id: ELECTION_ID,
          lao: mockLaoId,
          name: NAME,
          createdAt: TIMESTAMP_PAST1,
          start: TIMESTAMP_PAST1,
          end: TIMESTAMP_PAST2,
          questions: QUESTIONS,
          registeredVotes: REGISTERED_VOTES,
          electionStatus: ElectionStatus.NOT_STARTED,
        });
      expect(createWrongElection).toThrow(Error);
    });

    it('throws an error when created_at is undefined', () => {
      const createWrongElection = () =>
        new Election({
          id: ELECTION_ID,
          lao: mockLaoId,
          name: NAME,
          version: ElectionVersion.OPEN_BALLOT,
          start: TIMESTAMP_PAST1,
          end: TIMESTAMP_PAST2,
          questions: QUESTIONS,
          registeredVotes: REGISTERED_VOTES,
          electionStatus: ElectionStatus.NOT_STARTED,
        });
      expect(createWrongElection).toThrow(Error);
    });

    it('throws an error when start is undefined', () => {
      const createWrongElection = () =>
        new Election({
          id: ELECTION_ID,
          lao: mockLaoId,
          name: NAME,
          version: ElectionVersion.OPEN_BALLOT,
          createdAt: TIMESTAMP_PAST1,
          end: TIMESTAMP_PAST2,
          questions: QUESTIONS,
          registeredVotes: REGISTERED_VOTES,
          electionStatus: ElectionStatus.NOT_STARTED,
        });
      expect(createWrongElection).toThrow(Error);
    });

    it('throws an error when end is undefined', () => {
      const createWrongElection = () =>
        new Election({
          id: ELECTION_ID,
          lao: mockLaoId,
          name: NAME,
          version: ElectionVersion.OPEN_BALLOT,
          createdAt: TIMESTAMP_PAST1,
          start: TIMESTAMP_PAST1,
          questions: QUESTIONS,
          registeredVotes: REGISTERED_VOTES,
        });
      expect(createWrongElection).toThrow(Error);
    });

    it('throws an error when questions is undefined', () => {
      const createWrongElection = () =>
        new Election({
          id: ELECTION_ID,
          lao: mockLaoId,
          name: NAME,
          version: ElectionVersion.OPEN_BALLOT,
          createdAt: TIMESTAMP_PAST1,
          start: TIMESTAMP_PAST1,
          end: TIMESTAMP_PAST2,
          registeredVotes: REGISTERED_VOTES,
          electionStatus: ElectionStatus.NOT_STARTED,
        });
      expect(createWrongElection).toThrow(Error);
    });

    it('creates an election when registered_votes is undefined', () => {
      const e = new Election({
        id: ELECTION_ID,
        lao: mockLaoId,
        name: NAME,
        version: ElectionVersion.OPEN_BALLOT,
        createdAt: TIMESTAMP_PAST1,
        start: TIMESTAMP_PAST1,
        end: TIMESTAMP_PAST2,
        questions: QUESTIONS,
        electionStatus: ElectionStatus.NOT_STARTED,
      });
      const expected = new Election({
        id: ELECTION_ID,
        lao: mockLaoId,
        name: NAME,
        version: ElectionVersion.OPEN_BALLOT,
        createdAt: TIMESTAMP_PAST1,
        start: TIMESTAMP_PAST1,
        end: TIMESTAMP_PAST2,
        questions: QUESTIONS,
        registeredVotes: [],
        electionStatus: ElectionStatus.NOT_STARTED,
      });
      expect(e).toStrictEqual(expected);
    });
  });
});

afterAll(() => {
  jest.useRealTimers();
});
