import 'jest-extended';
import '__tests__/utils/matchers';
import {
  mockLaoId,
  mockLaoIdHash,
  mockLaoName,
  configureTestFeatures,
  mockKeyPair,
  mockLaoState,
} from '__tests__/utils';

import { Hash, Timestamp, Base64UrlData, Signature } from 'core/objects';
import { ActionType, MessageData, ObjectType } from 'core/network/jsonrpc/messages';

import STRINGS from 'resources/strings';
import { getStore, dispatch } from 'core/redux';
import { connectToLao } from 'features/lao/reducer';
import { addEvent } from 'features/events/reducer';
import { getEventFromId } from 'features/events/network/EventHandlerUtils';
import { Election, ElectionStatus } from 'features/evoting/objects';
import { handleElectionOpenMessage } from '../ElectionHandler';

const store = getStore();

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
  data: Base64UrlData.encode(''),
  sender: mockKeyPair.publicKey,
  signature: Base64UrlData.encode('') as Signature,
  channel: '',
  message_id: Hash.fromString(''),
  witness_signatures: [],
};

const initializeData = () => {
  // clear data in the redux store
  dispatch({ type: 'CLEAR_STORAGE', value: {} });
};

beforeAll(() => {
  configureTestFeatures();
  initializeData();
});

describe('ElectionHandler', () => {
  describe('election#open', () => {
    it('should return false if the object is not "election"', () => {
      const mockFn = jest.fn();
      console.warn = mockFn;

      expect(
        handleElectionOpenMessage({
          ...mockMessageData,
          messageData: {
            object: ObjectType.CHIRP,
            action: ActionType.OPEN,
          },
        }),
      ).toBeFalse();

      expect(mockFn).toHaveBeenCalledTimes(1);
      // check if the printed warning message contains substring
      expect(mockFn.mock.calls[0][0]).toMatch(/unsupported message/i);
    });
    it('should return false if the action is not "open"', () => {
      const mockFn = jest.fn();
      console.warn = mockFn;

      expect(
        handleElectionOpenMessage({
          ...mockMessageData,
          messageData: {
            object: ObjectType.ELECTION,
            action: ActionType.ADD,
          },
        }),
      ).toBeFalse();

      expect(mockFn).toHaveBeenCalledTimes(1);
      // check if the printed warning message contains substring
      expect(mockFn.mock.calls[0][0]).toMatch(/unsupported message/i);
    });
    it('should return false if there is no active LAO', () => {
      const mockFn = jest.fn();
      console.warn = mockFn;

      expect(
        handleElectionOpenMessage({
          ...mockMessageData,
          messageData: {
            object: ObjectType.ELECTION,
            action: ActionType.OPEN,
          },
        }),
      ).toBeFalse();

      expect(mockFn).toHaveBeenCalledTimes(1);
      // check if the printed warning message contains substring
      expect(mockFn.mock.calls[0][0]).toMatch(/LAO/i);
    });
    it('should return false if there the message LAO does not match the current LAO', () => {
      const mockFn = jest.fn();
      console.warn = mockFn;

      dispatch(connectToLao(mockLaoState));

      expect(
        handleElectionOpenMessage({
          ...mockMessageData,
          laoId: Hash.fromString('some garbage id'),
          messageData: {
            object: ObjectType.ELECTION,
            action: ActionType.OPEN,
          },
        }),
      ).toBeFalse();

      expect(mockFn).toHaveBeenCalledTimes(1);
      // check if the printed warning message contains substring
      expect(mockFn.mock.calls[0][0]).toMatch(/current LAO/i);
    });

    it('should return false if the election has not previously been stored', () => {
      const mockFn = jest.fn();
      console.warn = mockFn;

      dispatch(connectToLao(mockLaoState));

      expect(
        handleElectionOpenMessage({
          ...mockMessageData,
          messageData: {
            object: ObjectType.ELECTION,
            action: ActionType.OPEN,
            election: mockElectionId.valueOf(),
            opened_at: TIMESTAMP,
          } as MessageData,
        }),
      ).toBeFalse();

      expect(mockFn).toHaveBeenCalledTimes(1);
      // check if the printed warning message contains substring
      expect(mockFn.mock.calls[0][0]).toMatch(/election/i);
    });

    it('should update the election status', () => {
      const mockFn = jest.fn();
      console.warn = mockFn;

      dispatch(connectToLao(mockLaoState));
      dispatch(addEvent(mockLaoId, election.toState()));

      expect((getEventFromId(store.getState(), mockElectionId) as Election).electionStatus).toEqual(
        ElectionStatus.NOT_STARTED,
      );

      expect(
        handleElectionOpenMessage({
          ...mockMessageData,
          messageData: {
            object: ObjectType.ELECTION,
            action: ActionType.OPEN,
            election: mockElectionId.valueOf(),
            opened_at: TIMESTAMP,
          } as MessageData,
        }),
      ).toBeTrue();

      // no warning should have been printed
      expect(mockFn).toHaveBeenCalledTimes(0);

      // check if the status was changed correctly
      const e = getEventFromId(store.getState(), mockElectionId) as Election;
      expect(e.electionStatus).toEqual(ElectionStatus.RUNNING);
    });
  });
});
