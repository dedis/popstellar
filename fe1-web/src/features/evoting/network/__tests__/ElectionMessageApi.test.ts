import 'jest-extended';

import { combineReducers, createStore } from 'redux';

import {
  mockAddress,
  mockChannel,
  mockKeyPair,
  mockLaoId,
  mockLaoIdHash,
  mockPopToken,
} from '__tests__/utils';
import { publish, subscribeToChannel } from 'core/network';
import { addMessages, messageReducer } from 'core/network/ingestion';
import { ExtendedMessage } from 'core/network/ingestion/ExtendedMessage';
import {
  ActionType,
  configureMessages,
  MessageRegistry,
  ObjectType,
} from 'core/network/jsonrpc/messages';
import { channelFromIds, Timestamp } from 'core/objects';
import { dispatch } from 'core/redux';
import { mockElectionNotStarted, mockElectionOpened } from 'features/evoting/__tests__/utils';
import { SelectedBallots } from 'features/evoting/objects';
import {
  addElectionKeyMessage,
  clearAllElectionKeyMessages,
  electionKeyReducer,
} from 'features/evoting/reducer';

import {
  castVote,
  openElection,
  requestCreateElection,
  requestElectionKey,
  terminateElection,
} from '../ElectionMessageApi';
import { CastVote, EndElection, SetupElection } from '../messages';
import { ElectionKey } from '../messages/ElectionKey';
import { OpenElection } from '../messages/OpenElection';
import { RequestElectionKey } from '../messages/RequestElectionKey';

jest.mock('core/objects', () => {
  return {
    ...jest.requireActual('core/objects'),
    channelFromIds: jest.fn().mockImplementation(() => mockChannel),
  };
});

const mockStore = createStore(combineReducers({ ...messageReducer, ...electionKeyReducer }));

jest.mock('core/redux', () => {
  return {
    ...jest.requireActual('core/redux'),
    store: mockStore,
    getStore: () => mockStore,
    dispatch: (action: any) => mockStore.dispatch(action),
  };
});

jest.mock('core/network', () => {
  return {
    ...jest.requireActual('core/network'),
    publish: jest.fn(Promise.resolve),
    subscribe: jest.fn(Promise.resolve),
    subscribeToChannel: jest.fn(Promise.resolve),
    unsubscribeFromChannel: jest.fn(Promise.resolve),
  };
});

const mockRegistry = new MessageRegistry();
const handleElectionKeyMessage = jest.fn();
const handleElectionRequestKeyMessage = jest.fn();
mockRegistry.add(
  ObjectType.ELECTION,
  ActionType.KEY,
  handleElectionKeyMessage,
  ElectionKey.fromJson,
);
mockRegistry.add(
  ObjectType.ELECTION,
  ActionType.REQUEST_KEY,
  handleElectionRequestKeyMessage,
  RequestElectionKey.fromJson,
);
configureMessages(mockRegistry);

afterEach(() => {
  jest.clearAllMocks();
  mockStore.dispatch(clearAllElectionKeyMessages());
});

describe('requestElectionKey', () => {
  it('throws an error if the stored message is not of type election#key', async () => {
    const mockElectionKeyChannel = `/root/${mockLaoId}/election/key`;
    const promise = requestElectionKey(
      mockLaoIdHash,
      mockElectionNotStarted.id,
      mockKeyPair.publicKey,
    );
    expect(promise).toBeInstanceOf(Promise);

    // the function should setup a store watcher, wait for it to do so
    await new Promise((resolve) => setTimeout(resolve, 500));

    // then trigger it USING A WRONG MESSAGE
    const msg = ExtendedMessage.fromMessage(
      ExtendedMessage.fromData(
        new RequestElectionKey({
          election: mockElectionNotStarted.id,
        }),
        mockKeyPair,
      ),
      mockElectionKeyChannel,
      mockAddress,
    );
    dispatch(addMessages([msg.toState()]));

    dispatch(
      addElectionKeyMessage({
        electionId: mockElectionNotStarted.id.valueOf(),
        messageId: msg.message_id.valueOf(),
      }),
    );

    // after that, the function should unsubscribe from the channel and resolve the promise to an election key
    await expect(promise).toReject();
  });

  it('throws an error if the stored message was not sent by the organizer', async () => {
    const mockElectionKeyChannel = `/root/${mockLaoId}/election/key`;
    const promise = requestElectionKey(
      mockLaoIdHash,
      mockElectionNotStarted.id,
      mockKeyPair.publicKey,
    );
    expect(promise).toBeInstanceOf(Promise);

    // the function should setup a store watcher, wait for it to do so
    await new Promise((resolve) => setTimeout(resolve, 500));

    // then trigger it
    const msg = ExtendedMessage.fromMessage(
      ExtendedMessage.fromData(
        new ElectionKey({
          election: mockElectionNotStarted.id,
          election_key: mockPopToken.publicKey,
        }),
        mockPopToken, // not the organizer's public key
      ),
      mockElectionKeyChannel,
      mockAddress,
    );
    dispatch(addMessages([msg.toState()]));

    dispatch(
      addElectionKeyMessage({
        electionId: mockElectionNotStarted.id.valueOf(),
        messageId: msg.message_id.valueOf(),
      }),
    );

    // after that, the function should unsubscribe from the channel and resolve the promise to an election key
    await expect(promise).toReject();
  });

  it('works as expected using a valid set of parameters', async () => {
    const mockElectionKeyChannel = `/root/${mockLaoId}/election/key`;
    const promise = requestElectionKey(
      mockLaoIdHash,
      mockElectionNotStarted.id,
      mockKeyPair.publicKey,
    );
    expect(promise).toBeInstanceOf(Promise);

    // the function should setup a store watcher, wait for it to do so
    await new Promise((resolve) => setTimeout(resolve, 500));

    // then trigger it
    const msg = ExtendedMessage.fromMessage(
      ExtendedMessage.fromData(
        new ElectionKey({
          election: mockElectionNotStarted.id,
          election_key: mockPopToken.publicKey,
        }),
        mockKeyPair,
      ),
      mockElectionKeyChannel,
      mockAddress,
    );
    dispatch(addMessages([msg.toState()]));

    dispatch(
      addElectionKeyMessage({
        electionId: mockElectionNotStarted.id.valueOf(),
        messageId: msg.message_id.valueOf(),
      }),
    );

    // after that, the function should unsubscribe from the channel and resolve the promise to an election key
    expect((await promise).valueOf()).toEqual(mockPopToken.publicKey.valueOf());

    expect(subscribeToChannel).toHaveBeenCalledWith(mockElectionKeyChannel, undefined, false);
    expect(subscribeToChannel).toHaveBeenCalledTimes(1);

    const requestKeyMessage = new RequestElectionKey({
      election: mockElectionNotStarted.id,
    });

    expect(publish).toHaveBeenLastCalledWith(mockElectionKeyChannel, requestKeyMessage);
    expect(publish).toHaveBeenCalledTimes(1);
  });
});

describe('requestCreateElection', () => {
  it('works as expected using a valid set of parameters', () => {
    requestCreateElection(
      mockLaoIdHash,
      mockElectionNotStarted.name,
      mockElectionNotStarted.version,
      mockElectionNotStarted.start,
      mockElectionNotStarted.end,
      mockElectionNotStarted.questions,
      mockElectionNotStarted.createdAt,
      mockKeyPair.publicKey,
    );

    expect(channelFromIds).toHaveBeenCalledWith(mockLaoIdHash);
    expect(channelFromIds).toHaveBeenCalledTimes(1);

    const setupElectionMessage = new SetupElection({
      lao: mockElectionNotStarted.lao,
      id: mockElectionNotStarted.id,
      name: mockElectionNotStarted.name,
      version: mockElectionNotStarted.version,
      created_at: mockElectionNotStarted.createdAt,
      start_time: mockElectionNotStarted.start,
      end_time: mockElectionNotStarted.end,
      questions: mockElectionNotStarted.questions,
    });

    expect(publish).toHaveBeenLastCalledWith(mockChannel, setupElectionMessage);
    expect(publish).toHaveBeenCalledTimes(1);
  });
});

describe('openElection', () => {
  it('works as expected using a valid set of parameters', () => {
    openElection(mockLaoIdHash, mockElectionNotStarted);

    expect(channelFromIds).toHaveBeenCalledWith(mockLaoIdHash, mockElectionNotStarted.id);
    expect(channelFromIds).toHaveBeenCalledTimes(1);

    // cannot directly match the openElection message here as openedAt is set inside the openElection function
    expect(publish).toHaveBeenCalledWith(mockChannel, expect.anything());

    // of the first call [0] to publish, extract the second argument [1]
    const openElectionMessage = (publish as jest.Mock).mock.calls[0][1];
    expect(openElectionMessage).toBeInstanceOf(OpenElection);
    expect(openElectionMessage.election).toEqual(mockElectionNotStarted.id);
    expect(openElectionMessage.lao).toEqual(mockElectionNotStarted.lao);
    expect(openElectionMessage.opened_at.valueOf()).toBeLessThanOrEqual(
      Timestamp.EpochNow().valueOf(),
    );

    expect(publish).toHaveBeenCalledTimes(1);
  });
});

describe('castVote', () => {
  it('works as expected using a valid set of parameters', () => {
    const selectedBallots: SelectedBallots = { 0: new Set([0]), 1: new Set([1]) };

    castVote(mockLaoIdHash, mockElectionNotStarted, selectedBallots);

    expect(channelFromIds).toHaveBeenCalledWith(mockLaoIdHash, mockElectionNotStarted.id);
    expect(channelFromIds).toHaveBeenCalledTimes(1);

    // cannot directly match the openElection message here as created_at is set inside the openElection function
    expect(publish).toHaveBeenCalledWith(mockChannel, expect.anything());

    // of the first call [0] to publish, extract the second argument [1]
    const castVoteMessage = (publish as jest.Mock).mock.calls[0][1] as CastVote;
    expect(castVoteMessage).toBeInstanceOf(CastVote);
    expect(castVoteMessage.election).toEqual(mockElectionNotStarted.id);
    expect(castVoteMessage.lao).toEqual(mockElectionNotStarted.lao);
    expect(castVoteMessage.created_at.valueOf()).toBeLessThanOrEqual(
      Timestamp.EpochNow().valueOf(),
    );
    expect(castVoteMessage.votes).toEqual(
      CastVote.selectedBallotsToVotes(mockElectionNotStarted, selectedBallots),
    );

    expect(publish).toHaveBeenCalledTimes(1);
  });
});

describe('terminateElection', () => {
  it('works as expected using a valid set of parameters', () => {
    terminateElection(mockLaoIdHash, mockElectionOpened);

    expect(channelFromIds).toHaveBeenCalledWith(mockLaoIdHash, mockElectionOpened.id);
    expect(channelFromIds).toHaveBeenCalledTimes(1);

    // cannot directly match the openElection message here as created_at is set inside the openElection function
    expect(publish).toHaveBeenCalledWith(mockChannel, expect.anything());

    // of the first call [0] to publish, extract the second argument [1]
    const endElectionMessage = (publish as jest.Mock).mock.calls[0][1] as EndElection;

    expect((publish as jest.Mock).mock.calls[0][1]).toBeInstanceOf(EndElection);
    expect(endElectionMessage.election).toEqual(mockElectionOpened.id);
    expect(endElectionMessage.lao).toEqual(mockElectionOpened.lao);
    expect(endElectionMessage.created_at.valueOf()).toBeLessThanOrEqual(
      Timestamp.EpochNow().valueOf(),
    );
    expect(endElectionMessage.registered_votes).toEqual(
      EndElection.computeRegisteredVotesHash(mockElectionOpened),
    );

    expect(publish).toHaveBeenCalledTimes(1);
  });
});
