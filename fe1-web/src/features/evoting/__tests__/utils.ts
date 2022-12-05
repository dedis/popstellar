import { mockLaoId } from '__tests__/utils';
import { Base64UrlData, EventTags, Hash, PublicKey, Timestamp } from 'core/objects';
import { CastVote, ElectionResult, EndElection } from 'features/evoting/network/messages';
import STRINGS from 'resources/strings';

import {
  Election,
  ElectionStatus,
  Question,
  RegisteredVote,
  Vote,
  ElectionVersion,
  QuestionResult,
} from '../objects';
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
  mockElectionId,
  mockQuestion1,
);

export const mockQuestionId2 = Hash.fromStringArray(
  EventTags.QUESTION,
  mockElectionId,
  mockQuestion2,
);

export const mockBallotOption1 = 'Ballot Option 1';
export const mockBallotOption2 = 'Ballot Option 2';
export const mockBallotOptions = [mockBallotOption1, mockBallotOption2];

export const mockQuestionObject1 = new Question({
  id: mockQuestionId1,
  question: mockQuestion1,
  voting_method: STRINGS.election_method_Plurality,
  ballot_options: mockBallotOptions,
  write_in: false,
});

export const mockQuestionObject2 = new Question({
  id: mockQuestionId2,
  question: mockQuestion2,
  voting_method: STRINGS.election_method_Plurality,
  ballot_options: mockBallotOptions,
  write_in: false,
});

export const mockQuestions: Question[] = [mockQuestionObject1, mockQuestionObject2];

export const mockRegisteredVotes: RegisteredVote[] = [
  new RegisteredVote({
    createdAt: new Timestamp(0),
    messageId: new Hash('b'),
    sender: new PublicKey('sender 1'),
    votes: [
      new Vote({ id: new Hash('id1'), question: new Hash('q1'), vote: 1 }),
      new Vote({ id: new Hash('id2'), question: new Hash('q2'), vote: 0 }),
    ],
  }),
  new RegisteredVote({
    createdAt: new Timestamp(1),
    messageId: new Hash('a'),
    sender: new PublicKey('sender 2'),
    votes: [
      new Vote({ id: new Hash('id3'), question: new Hash('q3'), vote: 0 }),
      new Vote({ id: new Hash('id4'), question: new Hash('q4'), vote: 1 }),
    ],
  }),
  new RegisteredVote({
    createdAt: new Timestamp(2),
    messageId: new Hash('1a'),
    sender: new PublicKey('sender 3'),
    votes: [
      new Vote({ id: new Hash('id01'), question: new Hash('q01'), vote: 1 }),
      new Vote({ id: new Hash('id00'), question: new Hash('q00'), vote: 0 }),
      new Vote({ id: new Hash('id02'), question: new Hash('q02'), vote: 1 }),
    ],
  }),
];

export const mockElectionNotStarted = new Election({
  lao: mockLaoId,
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
  lao: mockLaoId,
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
  lao: mockLaoId,
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
  lao: mockLaoId,
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

export const mockVote1 = new Vote({
  id: mockVoteId1,
  question: mockQuestionId1,
  vote: 0,
});

export const mockVote2 = new Vote({
  id: mockVoteId2,
  question: mockQuestionId2,
  vote: 1,
});

export const mockVotes = [mockVote1];

export const mockElectionResultHash = Hash.fromStringArray(mockVoteId1);

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
  lao: mockLaoId,
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

export const mockElectionKeyState = 'uJz8E1KSoBTjJ1aG+WMrZX8RqFbW6OJBBobXydOoQmQ=';
export const mockEncodedElectionKey = new Base64UrlData(mockElectionKeyState);
export const mockElectionKey = new ElectionPublicKey(mockEncodedElectionKey);

export const mockElectionPrivateKeyString = 'o1EESXAvTFD34Ss29FVohukOximnyn/qf/PdZu2HCQw=';
export const mockEncodedElectionPrivateKey = new Base64UrlData(mockElectionPrivateKeyString);

export const mockElectionResults = new Election({
  lao: mockLaoId,
  id: mockElectionId,
  name: mockElectionName,
  version: ElectionVersion.OPEN_BALLOT,
  createdAt: TIMESTAMP,
  start: TIMESTAMP,
  end: CLOSE_TIMESTAMP,
  questions: mockQuestions,
  electionStatus: ElectionStatus.RESULT,
  registeredVotes: mockRegisteredVotes,
  questionResult: mockElectionResultQuestions.map(
    (q) =>
      new QuestionResult({
        id: new Hash(q.id),
        result: q.result.map((r) => ({ ballotOption: r.ballot_option, count: r.count })),
      }),
  ),
});
