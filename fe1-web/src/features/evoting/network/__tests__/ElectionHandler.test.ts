import 'jest-extended';
import '__tests__/utils/matchers';
import {
  mockLaoId,
  mockLaoIdHash,
  mockLaoName,
  configureTestFeatures,
  mockKeyPair,
  mockReduxAction,
} from '__tests__/utils';

import { Hash, Timestamp, Base64UrlData, Signature } from 'core/objects';
import { ActionType, MessageData, ObjectType } from 'core/network/jsonrpc/messages';

import STRINGS from 'resources/strings';
import { dispatch } from 'core/redux';
import { Election, ElectionStatus, EvotingConfiguration } from 'features/evoting/objects';
import { handleElectionOpenMessage } from '../ElectionHandler';

const TIMESTAMP = new Timestamp(1609455600); // 1st january 2021
const CLOSE_TIMESTAMP = new Timestamp(1609542000); // 2nd january 2021

const mockElectionId = Hash.fromStringArray(
  'Election',
  mockLaoId,
  TIMESTAMP.toString(),
  mockLaoName,
);

const election = new Election({
  lao: mockLaoIdHash,
  id: mockElectionId,
  name: 'An election',
  version: STRINGS.election_version_identifier,
  createdAt: TIMESTAMP,
  start: TIMESTAMP,
  end: CLOSE_TIMESTAMP,
  questions: [],
  electionStatus: ElectionStatus.NOT_STARTED,
  registeredVotes: [],
});

const mockMessageData = {
  receivedAt: TIMESTAMP,
  laoId: mockLaoIdHash,
  data: Base64UrlData.encode('some data'),
  sender: mockKeyPair.publicKey,
  signature: Base64UrlData.encode('some data') as Signature,
  channel: '',
  message_id: Hash.fromString('some string'),
  witness_signatures: [],
};

const getEventFromIdDummy: EvotingConfiguration['getEventFromId'] = () => undefined;
const updateEventDummy: EvotingConfiguration['updateEvent'] = () => mockReduxAction;

// mocks
const warn = jest.spyOn(console, 'warn').mockImplementation(() => {});

beforeAll(() => {
  configureTestFeatures();
});

beforeEach(() => {
  // clear data in the redux store
  dispatch({ type: 'CLEAR_STORAGE', value: {} });
});

afterEach(() => {
  warn.mockClear();
});

describe('ElectionHandler', () => {
  describe('election#open', () => {
    it('should return false if the object is not "election"', () => {
      expect(
        handleElectionOpenMessage(
          getEventFromIdDummy,
          updateEventDummy,
        )({
          ...mockMessageData,
          messageData: {
            object: ObjectType.CHIRP,
            action: ActionType.OPEN,
          },
        }),
      ).toBeFalse();

      expect(warn).toHaveBeenCalledTimes(1);
      // check if the printed warning message contains substring
      expect(warn.mock.calls[0][0]).toMatch(/unsupported message/i);
    });
    it('should return false if the action is not "open"', () => {
      expect(
        handleElectionOpenMessage(
          getEventFromIdDummy,
          updateEventDummy,
        )({
          ...mockMessageData,
          messageData: {
            object: ObjectType.ELECTION,
            action: ActionType.ADD,
          },
        }),
      ).toBeFalse();

      expect(warn).toHaveBeenCalledTimes(1);
      // check if the printed warning message contains substring
      expect(warn.mock.calls[0][0]).toMatch(/unsupported message/i);
    });

    it('should return false if the election has not previously been stored', () => {
      expect(
        handleElectionOpenMessage(
          getEventFromIdDummy,
          updateEventDummy,
        )({
          ...mockMessageData,
          messageData: {
            object: ObjectType.ELECTION,
            action: ActionType.OPEN,
            election: mockElectionId.valueOf(),
            opened_at: TIMESTAMP,
          } as MessageData,
        }),
      ).toBeFalse();

      expect(warn).toHaveBeenCalledTimes(1);
      // check if the printed warning message contains substring
      expect(warn.mock.calls[0][0]).toMatch(/election/i);
    });

    it('should update the election status', () => {
      let storedElection = election.toState();

      const getEventFromId = jest.fn().mockImplementation(() => Election.fromState(storedElection));
      const updateEvent = jest.fn().mockImplementation((laoId, eventState) => {
        storedElection = eventState;

        // Return a redux action, should be an action creator
        return mockReduxAction;
      });

      expect(storedElection.electionStatus).toEqual(ElectionStatus.NOT_STARTED);

      expect(
        handleElectionOpenMessage(
          getEventFromId,
          updateEvent,
        )({
          ...mockMessageData,
          messageData: {
            object: ObjectType.ELECTION,
            action: ActionType.OPEN,
            election: mockElectionId,
            opened_at: TIMESTAMP,
          } as MessageData,
        }),
      ).toBeTrue();

      // no warning should have been printed
      expect(warn).toHaveBeenCalledTimes(0);

      // check whether getEventFromId has been called correctly
      expect(getEventFromId.mock.calls[0][0]).toEqual(mockElectionId);
      expect(getEventFromId).toHaveBeenCalledTimes(1);

      // check whether updateEvent has been called correctly
      expect(updateEvent.mock.calls[0][0]).toEqual(mockLaoIdHash);
      expect(updateEvent.mock.calls[0][1]).toHaveProperty('id', election.id.valueOf());
      expect(updateEvent.mock.calls[0][1]).toHaveProperty('electionStatus', ElectionStatus.OPENED);
      expect(updateEvent).toHaveBeenCalledTimes(1);

      // check if the status was changed correctly
      expect(storedElection.electionStatus).toEqual(ElectionStatus.OPENED);
    });
  });
});
