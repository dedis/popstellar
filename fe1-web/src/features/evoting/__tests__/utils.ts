import { mockLaoId, mockLaoIdHash } from '__tests__/utils';
import { Base64UrlData, EventTags, Hash, Timestamp } from 'core/objects';
import { CastVote, ElectionResult, EndElection } from 'features/evoting/network/messages';
import STRINGS from 'resources/strings';

import {
  Election,
  ElectionStatus,
  Question,
  RegisteredVote,
  Vote,
  ElectionVersion,
} from "../objects";
import { ElectionPublicKey } from '../objects/ElectionPublicKey';

const TIMESTAMP = new Timestamp(1609455600); // 1st january 2021
const CLOSE_TIMESTAMP = new Timestamp(1609542000); // 2nd january 2021

export const mockElectionName = 'An election';

export const mockElectionId = Hash.fromStringArray(
  EventTags.ELECTION,
  mockLaoId,
  TIMESTAMP.toString(),
  mockElectionName,
);

export const mockQuestion1 = 'Mock Question 1';
export const mockQuestion2 = 'Mock Question 2';

export const mockQuestionId1: Hash = Hash.fromStringArray(
  EventTags.QUESTION,
  mockElectionId.toString(),
  mockQuestion1,
);

export const mockQuestionId2 = Hash.fromStringArray(
  EventTags.QUESTION,
  mockElectionId.toString(),
  mockQuestion2,
);

export const mockBallotOption1 = 'Ballot Option 1';
export const mockBallotOption2 = 'Ballot Option 2';
export const mockBallotOptions = [mockBallotOption1, mockBallotOption2];

export const mockQuestionObject1: Question = {
  id: mockQuestionId1.toString(),
  question: mockQuestion1,
  voting_method: STRINGS.election_method_Plurality,
  ballot_options: mockBallotOptions,
  write_in: false,
};

export const mockQuestionObject2: Question = {
  id: mockQuestionId2.toString(),
  question: mockQuestion2,
  voting_method: STRINGS.election_method_Plurality,
  ballot_options: mockBallotOptions,
  write_in: false,
};

export const mockQuestions: Question[] = [mockQuestionObject1, mockQuestionObject2];

export const mockRegisteredVotes: RegisteredVote[] = [
  {
    createdAt: 0,
    messageId: 'b',
    sender: 'sender 1',
    votes: [
      { id: 'id1', question: 'q1', vote: 0 },
      { id: 'id2', question: 'q2', vote: 0 },
    ],
  },
  {
    createdAt: 1,
    messageId: 'a',
    sender: 'sender 2',
    votes: [
      { id: 'id3', question: 'q3', vote: 0 },
      { id: 'id4', question: 'q4', vote: 0 },
    ],
  },
];

export const mockElectionNotStarted = new Election({
  lao: mockLaoIdHash,
  id: mockElectionId,
  name: mockElectionName,
  version: ElectionVersion.OPEN_BALLOT,
  createdAt: TIMESTAMP,
  start: TIMESTAMP,
  end: CLOSE_TIMESTAMP,
  questions: mockQuestions,
  electionStatus: ElectionStatus.NOT_STARTED,
});

export const mockSecretBallotElectionNotStarted = new Election({
  lao: mockLaoIdHash,
  id: mockElectionId,
  name: mockElectionName,
  version: ElectionVersion.SECRET_BALLOT,
  createdAt: TIMESTAMP,
  start: TIMESTAMP,
  end: CLOSE_TIMESTAMP,
  questions: mockQuestions,
  electionStatus: ElectionStatus.NOT_STARTED,
});

export const mockElectionOpened = new Election({
  lao: mockLaoIdHash,
  id: mockElectionId,
  name: mockElectionName,
  version: ElectionVersion.OPEN_BALLOT,
  createdAt: TIMESTAMP,
  start: TIMESTAMP,
  end: CLOSE_TIMESTAMP,
  questions: mockQuestions,
  electionStatus: ElectionStatus.OPENED,
  registeredVotes: mockRegisteredVotes,
});

export const mockElectionTerminated = new Election({
  lao: mockLaoIdHash,
  id: mockElectionId,
  name: mockElectionName,
  version: ElectionVersion.OPEN_BALLOT,
  createdAt: TIMESTAMP,
  start: TIMESTAMP,
  end: CLOSE_TIMESTAMP,
  questions: mockQuestions,
  electionStatus: ElectionStatus.TERMINATED,
  registeredVotes: mockRegisteredVotes,
});

export const mockRegistedVotesHash = EndElection.computeRegisteredVotesHash(mockElectionOpened);

export const mockVoteOption1 = 0;
export const mockVoteOption2 = 1;

export const mockVoteId1 = CastVote.computeVoteId(mockElectionNotStarted, 0, mockVoteOption1);
export const mockVoteId2 = CastVote.computeVoteId(mockElectionNotStarted, 1, mockVoteOption2);

export const mockVote1: Vote = {
  id: mockVoteId1.toString(),
  question: mockQuestionId1.valueOf(),
  vote: 0,
};

export const mockVote2: Vote = {
  id: mockVoteId2.toString(),
  question: mockQuestionId2.valueOf(),
  vote: 1,
};

export const mockVotes = [mockVote1];

export const mockElectionResultHash = Hash.fromStringArray(mockVoteId1.valueOf());

export const mockElectionResultQuestions: ElectionResult['questions'] = [
  {
    id: mockQuestionId1.valueOf(),
    result: [
      { ballot_option: mockBallotOption1, count: 4 },
      { ballot_option: mockBallotOption2, count: 2 },
    ],
  },
  {
    id: mockQuestionId2.valueOf(),
    result: [
      { ballot_option: mockBallotOption1, count: 1 },
      { ballot_option: mockBallotOption2, count: 10 },
    ],
  },
];

export const openedSecretBallotElection = new Election({
  lao: mockLaoIdHash,
  id: mockElectionId,
  name: 'An election',
  version: ElectionVersion.SECRET_BALLOT,
  createdAt: TIMESTAMP,
  start: TIMESTAMP,
  end: CLOSE_TIMESTAMP,
  questions: mockQuestions,
  electionStatus: ElectionStatus.OPENED,
  registeredVotes: [],
});

export const mockElectionKeyString = 'uJz8E1KSoBTjJ1aG+WMrZX8RqFbW6OJBBobXydOoQmQ=';
export const mockEncodedElectionKey = new Base64UrlData(mockElectionKeyString);
export const mockElectionKey = new ElectionPublicKey(mockEncodedElectionKey);

export const mockElectionPrivateKeyString = 'o1EESXAvTFD34Ss29FVohukOximnyn/qf/PdZu2HCQw=';
export const mockEncodedElectionPrivateKey = new Base64UrlData(mockElectionPrivateKeyString);

export const mockElectionResults = new Election({
  lao: mockLaoIdHash,
  id: mockElectionId,
  name: mockElectionName,
  version: ElectionVersion.OPEN_BALLOT,
  createdAt: TIMESTAMP,
  start: TIMESTAMP,
  end: CLOSE_TIMESTAMP,
  questions: mockQuestions,
  electionStatus: ElectionStatus.RESULT,
  registeredVotes: mockRegisteredVotes,
  questionResult: mockElectionResultQuestions.map((q) => ({
    id: q.id,
    result: q.result.map((r) => ({ ballotOption: r.ballot_option, count: r.count })),
  })),
});
