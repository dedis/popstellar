import React from 'react';
import { render } from '@testing-library/react-native';

import { EventTags, Hash, Timestamp } from 'core/objects';
import { mockLaoId, mockLaoIdHash, mockLaoName } from '__tests__/utils';
import STRINGS from 'resources/strings';
import {
  Election,
  ElectionStatus,
  Question,
  QuestionResult,
  RegisteredVote,
} from 'features/evoting/objects';
import { configure } from 'features/evoting';
import EventElection from '../EventElection';

// region test data initialization

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

const registeredVote: RegisteredVote = {
  createdAt: 0,
  messageId: '',
  sender: '',
  votes: [{ id: '', question: '', vote: [0] }],
};

const questionResult: QuestionResult = {
  id: '',
  result: [
    { ballotOption: mockBallotOptions[0], count: 10 },
    { ballotOption: mockBallotOptions[1], count: 3 },
  ],
};

const notStartedElection = new Election({
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

const runningElection = new Election({
  lao: mockLaoIdHash,
  id: mockElectionId,
  name: 'An election',
  version: STRINGS.election_version_identifier,
  createdAt: TIMESTAMP,
  start: TIMESTAMP,
  end: CLOSE_TIMESTAMP,
  questions: [question],
  electionStatus: ElectionStatus.OPENED,
  registeredVotes: [registeredVote],
});

const terminatedElection = new Election({
  lao: mockLaoIdHash,
  id: mockElectionId,
  name: 'An election',
  version: STRINGS.election_version_identifier,
  createdAt: TIMESTAMP,
  start: TIMESTAMP,
  end: CLOSE_TIMESTAMP,
  questions: [question],
  electionStatus: ElectionStatus.TERMINATED,
  registeredVotes: [registeredVote],
});

const finishedElection = new Election({
  lao: mockLaoIdHash,
  id: mockElectionId,
  name: 'An election',
  version: STRINGS.election_version_identifier,
  createdAt: TIMESTAMP,
  start: TIMESTAMP,
  end: CLOSE_TIMESTAMP,
  questions: [question],
  electionStatus: ElectionStatus.FINISHED,
  registeredVotes: [registeredVote],
});

const resultElection = new Election({
  lao: mockLaoIdHash,
  id: mockElectionId,
  name: 'An election',
  version: STRINGS.election_version_identifier,
  createdAt: TIMESTAMP,
  start: TIMESTAMP,
  end: CLOSE_TIMESTAMP,
  questions: [question],
  electionStatus: ElectionStatus.RESULT,
  registeredVotes: [registeredVote],
  questionResult: [questionResult],
});

const undefinedElection = new Election({
  lao: mockLaoIdHash,
  id: mockElectionId,
  name: 'An election',
  version: STRINGS.election_version_identifier,
  createdAt: TIMESTAMP,
  start: TIMESTAMP,
  end: CLOSE_TIMESTAMP,
  questions: [question],
  electionStatus: 'undefined' as ElectionStatus,
  registeredVotes: [registeredVote],
  questionResult: [questionResult],
});

// endregion

// mocks
const warn = jest.spyOn(console, 'warn').mockImplementation(() => {});

beforeAll(() => {
  configure({ addEvent: () => undefined });
});
afterEach(() => {
  warn.mockClear();
});

describe('EventElection', () => {
  describe('Not started election', () => {
    it('renders correctly for an organizer', () => {
      const component = render(
        <EventElection election={notStartedElection} isOrganizer />,
      ).toJSON();
      expect(component).toMatchSnapshot();
    });

    it('renders correctly for an attendee', () => {
      const component = render(<EventElection election={notStartedElection} />).toJSON();
      expect(component).toMatchSnapshot();
    });
  });

  describe('Running election', () => {
    it('renders correctly for an organizer', () => {
      const component = render(<EventElection election={runningElection} isOrganizer />).toJSON();
      expect(component).toMatchSnapshot();
    });

    it('renders correctly for an attendee', () => {
      const component = render(<EventElection election={runningElection} />).toJSON();
      expect(component).toMatchSnapshot();
    });
  });

  describe('Terminated election where the results are not yet available', () => {
    it('renders correctly for an organizer', () => {
      const component = render(
        <EventElection election={terminatedElection} isOrganizer />,
      ).toJSON();
      expect(component).toMatchSnapshot();
    });

    it('renders correctly for an attendee', () => {
      const component = render(<EventElection election={terminatedElection} />).toJSON();
      expect(component).toMatchSnapshot();
    });
  });

  describe('Finished election where the results are not yet available', () => {
    it('renders correctly for an organizer', () => {
      const component = render(<EventElection election={finishedElection} isOrganizer />).toJSON();
      expect(component).toMatchSnapshot();
    });

    it('renders correctly for an attendee', () => {
      const component = render(<EventElection election={finishedElection} />).toJSON();
      expect(component).toMatchSnapshot();
    });
  });

  describe('Finished election where the results are available', () => {
    it('renders correctly for an organizer', () => {
      const component = render(<EventElection election={resultElection} isOrganizer />).toJSON();
      expect(component).toMatchSnapshot();
    });

    it('renders correctly for an attendee', () => {
      const component = render(<EventElection election={resultElection} />).toJSON();
      expect(component).toMatchSnapshot();
    });
  });

  describe('Undefined election status', () => {
    it('renders null for an organizer', () => {
      const component = render(<EventElection election={undefinedElection} isOrganizer />).toJSON();
      expect(component).toMatchSnapshot();

      expect(warn).toHaveBeenCalledTimes(1);
      // check if the printed warning message contains substring
      expect(warn.mock.calls[0][0]).toMatch(/undefined/i);
    });

    it('renders null for an attendee', () => {
      const component = render(<EventElection election={undefinedElection} />).toJSON();
      expect(component).toMatchSnapshot();

      expect(warn).toHaveBeenCalledTimes(1);
      // check if the printed warning message contains substring
      expect(warn.mock.calls[0][0]).toMatch(/undefined/i);
    });
  });
});
