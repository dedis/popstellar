import 'jest-extended';
import '__tests__/utils/matchers';

import {
  configureTestFeatures,
  mockKeyPair,
  mockLao,
  mockLaoIdHash,
  mockPopToken,
  mockReduxAction,
} from '__tests__/utils';
import { KeyPairStore } from 'core/keypair';
import { subscribeToChannel } from 'core/network';
import { ActionType, MessageData, ObjectType } from 'core/network/jsonrpc/messages';
import {
  Base64UrlData,
  channelFromIds,
  getLastPartOfChannel,
  Hash,
  Signature,
  Timestamp,
} from 'core/objects';
import { dispatch } from 'core/redux';
import { EvotingConfiguration } from 'features/evoting/interface';
import { Election, ElectionState, ElectionStatus, RegisteredVote } from 'features/evoting/objects';
import {
  mockElectionId,
  mockElectionNotStarted,
  mockElectionOpened,
  mockElectionResultQuestions,
  mockElectionTerminated,
  mockRegistedVotesHash,
  mockVote1,
  mockVote2,
} from 'features/evoting/objects/__tests__/utils';

import {
  handleCastVoteMessage,
  handleElectionEndMessage,
  handleElectionOpenMessage,
  handleElectionResultMessage,
  handleElectionSetupMessage,
} from '../ElectionHandler';
import { CastVote, ElectionResult, EndElection, SetupElection } from '../messages';
import { OpenElection } from '../messages/OpenElection';

const TIMESTAMP = new Timestamp(1609455600); // 1st january 2021

const mockMessageData = {
  receivedAt: TIMESTAMP,
  laoId: mockLaoIdHash,
  data: Base64UrlData.encode('some data'),
  sender: mockKeyPair.publicKey,
  signature: Base64UrlData.encode('some data') as Signature,
  channel: `some channel/${mockElectionId.valueOf()}`,
  message_id: Hash.fromString('some string'),
  witness_signatures: [],
};

const getMockLao: EvotingConfiguration['getCurrentLao'] = () => mockLao;
const getEventByIdDummy: EvotingConfiguration['getEventById'] = () => undefined;
const updateEventDummy: EvotingConfiguration['updateEvent'] = () => mockReduxAction;

// mock channelFromIds and subscribeToChannel (spyOn does not work)
const mockChannelId = 'someChannelId';

jest.mock('core/objects', () => {
  return {
    ...jest.requireActual('core/objects'),
    channelFromIds: jest.fn().mockImplementation(() => mockChannelId),
  };
});

jest.mock('core/network', () => {
  return {
    ...jest.requireActual('core/network'),
    subscribeToChannel: jest.fn().mockImplementation(() => Promise.resolve()),
  };
});

beforeAll(() => {
  configureTestFeatures();
});

beforeEach(() => {
  // clear data in the redux store
  dispatch({ type: 'CLEAR_STORAGE', value: {} });
});

afterEach(() => {
  jest.clearAllMocks();
});

describe('ElectionHandler', () => {
  describe('election#setup', () => {
    it('should return false if the object is not "election"', () => {
      const addEvent = jest.fn();

      expect(
        handleElectionSetupMessage(addEvent)({
          ...mockMessageData,
          messageData: {
            object: ObjectType.CHIRP,
            action: ActionType.SETUP,
          },
        }),
      ).toBeFalse();
    });
    it('should return false if the action is not "setup"', () => {
      const addEvent = jest.fn();

      expect(
        handleElectionSetupMessage(addEvent)({
          ...mockMessageData,
          messageData: {
            object: ObjectType.ELECTION,
            action: ActionType.ADD,
          },
        }),
      ).toBeFalse();
    });

    it('should create the election', () => {
      let storedElection: ElectionState | undefined;

      const addEvent = jest.fn().mockImplementation((laoId, eventState) => {
        storedElection = eventState;

        // Return a redux action, should be an action creator
        return mockReduxAction;
      });

      expect(storedElection).toEqual(undefined);

      expect(
        handleElectionSetupMessage(addEvent)({
          ...mockMessageData,
          messageData: new SetupElection({
            lao: mockLaoIdHash,
            id: mockElectionNotStarted.id,
            name: mockElectionNotStarted.name,
            version: mockElectionNotStarted.version,
            created_at: mockElectionNotStarted.createdAt,
            start_time: mockElectionNotStarted.start,
            end_time: mockElectionNotStarted.end,
            questions: mockElectionNotStarted.questions,
          }),
        }),
      ).toBeTrue();

      // it should have been subscripted to the election channel
      expect(channelFromIds).toHaveBeenCalledTimes(1);
      expect(channelFromIds).toHaveBeenCalledWith(
        mockElectionNotStarted.lao,
        mockElectionNotStarted.id,
      );

      expect(subscribeToChannel).toHaveBeenCalledTimes(1);
      expect(subscribeToChannel).toHaveBeenCalledWith(mockChannelId);

      // check whether updateEvent has been called correctly
      const newElectionState = {
        ...mockElectionNotStarted.toState(),
        electionStatus: ElectionStatus.NOT_STARTED,
      };

      expect(addEvent).toHaveBeenCalledWith(mockLaoIdHash, newElectionState);
      expect(addEvent).toHaveBeenCalledTimes(1);

      // check if the status was changed correctly
      expect(storedElection).toEqual(newElectionState);
    });
  });

  describe('election#open', () => {
    it('should return false if the object is not "election"', () => {
      expect(
        handleElectionOpenMessage(
          getEventByIdDummy,
          updateEventDummy,
        )({
          ...mockMessageData,
          messageData: {
            object: ObjectType.CHIRP,
            action: ActionType.OPEN,
          },
        }),
      ).toBeFalse();
    });
    it('should return false if the action is not "open"', () => {
      expect(
        handleElectionOpenMessage(
          getEventByIdDummy,
          updateEventDummy,
        )({
          ...mockMessageData,
          messageData: {
            object: ObjectType.ELECTION,
            action: ActionType.ADD,
          },
        }),
      ).toBeFalse();
    });

    it('should return false if the election has not previously been stored', () => {
      expect(
        handleElectionOpenMessage(
          getEventByIdDummy,
          updateEventDummy,
        )({
          ...mockMessageData,
          messageData: {
            object: ObjectType.ELECTION,
            action: ActionType.OPEN,
            election: mockElectionId.valueOf(),
            created_at: TIMESTAMP,
          } as MessageData,
        }),
      ).toBeFalse();
    });

    it('should update the election status', () => {
      let storedElection = mockElectionNotStarted.toState();

      const getEventById = jest.fn().mockImplementation(() => Election.fromState(storedElection));
      const updateEvent = jest.fn().mockImplementation((laoId, eventState) => {
        storedElection = eventState;

        // Return a redux action, should be an action creator
        return mockReduxAction;
      });

      expect(storedElection.electionStatus).toEqual(ElectionStatus.NOT_STARTED);

      expect(
        handleElectionOpenMessage(
          getEventById,
          updateEvent,
        )({
          ...mockMessageData,
          messageData: new OpenElection({
            lao: mockLaoIdHash,
            election: mockElectionId,
            opened_at: TIMESTAMP,
          }),
        }),
      ).toBeTrue();

      // check whether getEventById has been called correctly
      expect(getEventById).toHaveBeenCalledWith(mockElectionId);
      expect(getEventById).toHaveBeenCalledTimes(1);

      // check whether updateEvent has been called correctly
      const newElectionState = {
        ...mockElectionNotStarted.toState(),
        electionStatus: ElectionStatus.OPENED,
      };

      expect(updateEvent).toHaveBeenCalledWith(mockLaoIdHash, newElectionState);
      expect(updateEvent).toHaveBeenCalledTimes(1);

      // check if the status was changed correctly
      expect(storedElection).toEqual(newElectionState);
    });
  });

  describe('election#castVote', () => {
    it('should return false if the object is not "election"', () => {
      expect(
        handleCastVoteMessage(
          getMockLao,
          getEventByIdDummy,
          updateEventDummy,
        )({
          ...mockMessageData,
          messageData: {
            object: ObjectType.CHIRP,
            action: ActionType.CAST_VOTE,
          },
        }),
      ).toBeFalse();
    });
    it('should return false if the action is not "cast_vote"', () => {
      expect(
        handleCastVoteMessage(
          getMockLao,
          getEventByIdDummy,
          updateEventDummy,
        )({
          ...mockMessageData,
          messageData: {
            object: ObjectType.ELECTION,
            action: ActionType.ADD,
          },
        }),
      ).toBeFalse();
    });

    it('for attendees should return false if the election has not previously been stored', () => {
      // stores the keypair of somebody else
      KeyPairStore.store(mockPopToken);

      expect(
        handleCastVoteMessage(
          getMockLao,
          getEventByIdDummy,
          updateEventDummy,
        )({
          ...mockMessageData,
          messageData: {
            object: ObjectType.ELECTION,
            action: ActionType.CAST_VOTE,
            election: mockElectionId.valueOf(),
            opened_at: TIMESTAMP,
          } as MessageData,
        }),
      ).toBeTrue();
    });

    it('for organizers should return false if the election has not previously been stored', () => {
      // stores the keypair of the mockLao organizer
      KeyPairStore.store(mockKeyPair);

      expect(
        handleCastVoteMessage(
          getMockLao,
          getEventByIdDummy,
          updateEventDummy,
        )({
          ...mockMessageData,
          messageData: {
            object: ObjectType.ELECTION,
            action: ActionType.CAST_VOTE,
            election: mockElectionId.valueOf(),
            opened_at: TIMESTAMP,
          } as MessageData,
        }),
      ).toBeFalse();
    });

    it('for attendees should update election.registeredVotes', () => {
      // stores the keypair of somebody else
      KeyPairStore.store(mockPopToken);

      let storedElection = mockElectionOpened.toState();

      const getEventById = jest.fn().mockImplementation(() => Election.fromState(storedElection));
      const updateEvent = jest.fn().mockImplementation((laoId, eventState) => {
        storedElection = eventState;

        // Return a redux action, should be an action creator
        return mockReduxAction;
      });

      expect(storedElection.electionStatus).toEqual(ElectionStatus.OPENED);

      const castVoteMessage = new CastVote({
        lao: mockLaoIdHash,
        election: mockElectionId,
        created_at: TIMESTAMP,
        votes: [mockVote1, mockVote2],
      });

      expect(
        handleCastVoteMessage(
          getMockLao,
          getEventById,
          updateEvent,
        )({
          ...mockMessageData,
          messageData: castVoteMessage,
        }),
      ).toBeTrue();

      // check whether getEventById and updateEvent have been not been
      expect(getEventById).toHaveBeenCalledTimes(0);
      expect(updateEvent).toHaveBeenCalledTimes(0);

      // verify that the stored election was not changed
      expect(storedElection).toEqual(mockElectionOpened.toState());
    });

    it('for organizers should update election.registeredVotes', () => {
      // stores the keypair of the mockLao organizer
      KeyPairStore.store(mockKeyPair);

      let storedElection = mockElectionOpened.toState();

      const getEventById = jest.fn().mockImplementation(() => Election.fromState(storedElection));
      const updateEvent = jest.fn().mockImplementation((laoId, eventState) => {
        storedElection = eventState;

        // Return a redux action, should be an action creator
        return mockReduxAction;
      });

      expect(storedElection.electionStatus).toEqual(ElectionStatus.OPENED);

      const castVoteMessage = new CastVote({
        lao: mockLaoIdHash,
        election: mockElectionId,
        created_at: TIMESTAMP,
        votes: [mockVote1, mockVote2],
      });

      expect(
        handleCastVoteMessage(
          getMockLao,
          getEventById,
          updateEvent,
        )({
          ...mockMessageData,
          messageData: castVoteMessage,
        }),
      ).toBeTrue();

      // check whether getEventById has been called correctly
      expect(getEventById).toHaveBeenCalledWith(mockElectionId);
      expect(getEventById).toHaveBeenCalledTimes(1);

      const newVote: RegisteredVote = {
        createdAt: castVoteMessage.created_at.valueOf(),
        sender: mockMessageData.sender.valueOf(),
        votes: castVoteMessage.votes,
        messageId: mockMessageData.message_id.valueOf(),
      };

      const newRegisteredVotes = [...mockElectionOpened.registeredVotes, newVote];

      // check whether updateEvent has been called correctly
      const newElectionState = {
        ...mockElectionOpened.toState(),
        registeredVotes: newRegisteredVotes,
      };

      expect(updateEvent).toHaveBeenLastCalledWith(mockLaoIdHash, newElectionState);

      // check if the stored election was correctly updated
      expect(storedElection).toEqual(newElectionState);
    });
  });

  describe('election#end', () => {
    it('should return false if the object is not "election"', () => {
      expect(
        handleElectionEndMessage(
          getEventByIdDummy,
          updateEventDummy,
        )({
          ...mockMessageData,
          messageData: {
            object: ObjectType.CHIRP,
            action: ActionType.END,
          },
        }),
      ).toBeFalse();
    });
    it('should return false if the action is not "end"', () => {
      expect(
        handleElectionEndMessage(
          getEventByIdDummy,
          updateEventDummy,
        )({
          ...mockMessageData,
          messageData: {
            object: ObjectType.ELECTION,
            action: ActionType.ADD,
          },
        }),
      ).toBeFalse();
    });

    it('should return false if the election has not previously been stored', () => {
      expect(
        handleElectionEndMessage(
          getEventByIdDummy,
          updateEventDummy,
        )({
          ...mockMessageData,
          messageData: {
            object: ObjectType.ELECTION,
            action: ActionType.END,
            election: mockElectionId.valueOf(),
            created_at: TIMESTAMP,
          } as MessageData,
        }),
      ).toBeFalse();
    });

    it('should update the election status', () => {
      let storedElection = mockElectionOpened.toState();

      const getEventById = jest.fn().mockImplementation(() => Election.fromState(storedElection));
      const updateEvent = jest.fn().mockImplementation((laoId, eventState) => {
        storedElection = eventState;

        // Return a redux action, should be an action creator
        return mockReduxAction;
      });

      expect(storedElection.electionStatus).toEqual(ElectionStatus.OPENED);

      expect(
        handleElectionEndMessage(
          getEventById,
          updateEvent,
        )({
          ...mockMessageData,
          messageData: new EndElection({
            lao: mockLaoIdHash,
            election: mockElectionId,
            created_at: TIMESTAMP,
            registered_votes: mockRegistedVotesHash,
          }),
        }),
      ).toBeTrue();

      // check whether getEventById has been called correctly
      expect(getEventById).toHaveBeenCalledWith(mockElectionId);
      expect(getEventById).toHaveBeenCalledTimes(1);

      // check whether updateEvent has been called correctly
      const newElectionState = {
        ...mockElectionOpened.toState(),
        electionStatus: ElectionStatus.TERMINATED,
      };

      expect(updateEvent).toHaveBeenCalledWith(mockLaoIdHash, newElectionState);
      expect(updateEvent).toHaveBeenCalledTimes(1);

      // check if the status was changed correctly
      expect(storedElection).toEqual(newElectionState);
    });
  });

  describe('election#result', () => {
    it('should return false if the object is not "election"', () => {
      expect(
        handleElectionResultMessage(
          getEventByIdDummy,
          updateEventDummy,
        )({
          ...mockMessageData,
          messageData: {
            object: ObjectType.CHIRP,
            action: ActionType.RESULT,
          },
        }),
      ).toBeFalse();
    });

    it('should return false if the action is not "result"', () => {
      expect(
        handleElectionResultMessage(
          getEventByIdDummy,
          updateEventDummy,
        )({
          ...mockMessageData,
          messageData: {
            object: ObjectType.ELECTION,
            action: ActionType.ADD,
          },
        }),
      ).toBeFalse();
    });

    it('should return false if the message data does not contain a channel', () => {
      expect(
        handleElectionResultMessage(
          getEventByIdDummy,
          updateEventDummy,
        )({
          ...mockMessageData,
          channel: '',
          messageData: {
            object: ObjectType.ELECTION,
            action: ActionType.RESULT,
          },
        }),
      ).toBeFalse();
    });

    it('should return false if the election has not previously been stored', () => {
      expect(
        handleElectionResultMessage(
          getEventByIdDummy,
          updateEventDummy,
        )({
          ...mockMessageData,
          messageData: {
            object: ObjectType.ELECTION,
            action: ActionType.RESULT,
            election: mockElectionId.valueOf(),
            created_at: TIMESTAMP,
          } as MessageData,
        }),
      ).toBeFalse();
    });

    it('should update the election status and store results', () => {
      let storedElection = mockElectionTerminated.toState();

      const getEventById = jest.fn().mockImplementation(() => Election.fromState(storedElection));
      const updateEvent = jest.fn().mockImplementation((laoId, eventState) => {
        storedElection = eventState;

        // Return a redux action, should be an action creator
        return mockReduxAction;
      });

      expect(storedElection.electionStatus).toEqual(ElectionStatus.TERMINATED);

      expect(
        handleElectionResultMessage(
          getEventById,
          updateEvent,
        )({
          ...mockMessageData,
          messageData: new ElectionResult({
            questions: mockElectionResultQuestions,
          }),
        }),
      ).toBeTrue();

      // check whether getEventById has been called correctly
      expect(getEventById).toHaveBeenCalledWith(getLastPartOfChannel(mockMessageData.channel));
      expect(getEventById).toHaveBeenCalledTimes(1);

      // check whether updateEvent has been called correctly
      const newElectionState = {
        ...mockElectionTerminated.toState(),
        electionStatus: ElectionStatus.RESULT,
        questionResult: mockElectionResultQuestions.map((q) => ({
          id: q.id,
          result: q.result.map((r) => ({ ballotOption: r.ballot_option, count: r.count })),
        })),
      };

      expect(updateEvent).toHaveBeenCalledWith(mockLaoIdHash, newElectionState);

      // check if the results were correctly stored
      expect(storedElection).toEqual(newElectionState);
    });
  });
});
