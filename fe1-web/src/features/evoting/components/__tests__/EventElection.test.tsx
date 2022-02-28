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
import EventElection from '../EventElection';

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
  electionStatus: ElectionStatus.RUNNING,
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

const resultElection = new Election({
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
  questionResult: [questionResult],
});

beforeEach(() => {});

describe('EventElection', () => {
  describe('Not started election', () => {
    it('renders correctly for an organizer', () => {
      const component = render(
        <EventElection election={notStartedElection} isOrganizer />,
      ).toJSON();
      expect(component).toMatchSnapshot();
    });
    it('renders correctly for an attendee', () => {
      const component = render(
        <EventElection election={notStartedElection} isOrganizer={false} />,
      ).toJSON();
      expect(component).toMatchSnapshot();
    });
  });

  describe('Running election', () => {
    it('renders correctly for an organizer', () => {
      const component = render(<EventElection election={runningElection} isOrganizer />).toJSON();
      expect(component).toMatchSnapshot();
    });
    it('renders correctly for an attendee', () => {
      const component = render(
        <EventElection election={runningElection} isOrganizer={false} />,
      ).toJSON();
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
      const component = render(
        <EventElection election={terminatedElection} isOrganizer={false} />,
      ).toJSON();
      expect(component).toMatchSnapshot();
    });
  });

  describe('Terminated election where the results are available', () => {
    it('renders correctly for an organizer', () => {
      const component = render(<EventElection election={resultElection} isOrganizer />).toJSON();
      expect(component).toMatchSnapshot();
    });
    it('renders correctly for an attendee', () => {
      const component = render(
        <EventElection election={resultElection} isOrganizer={false} />,
      ).toJSON();
      expect(component).toMatchSnapshot();
    });
  });
});
