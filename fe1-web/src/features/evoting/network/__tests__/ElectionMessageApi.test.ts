import 'jest-extended';
import { configureStore } from '@reduxjs/toolkit';
import { combineReducers } from 'redux';

import { mockChannel, mockLaoId } from '__tests__/utils';
import { publish } from 'core/network';
import { messageReducer } from 'core/network/ingestion';
import { Base64UrlData, channelFromIds, Timestamp } from 'core/objects';
import {
  mockElectionNotStarted,
  mockElectionOpened,
  mockSecretBallotElectionNotStarted,
} from 'features/evoting/__tests__/utils';
import { SelectedBallots } from 'features/evoting/objects';
import { ElectionKeyPair } from 'features/evoting/objects/ElectionKeyPair';
import { electionKeyReducer } from 'features/evoting/reducer';

import {
  castVote,
  openElection,
  requestCreateElection,
  terminateElection,
} from '../ElectionMessageApi';
import { CastVote, EndElection, SetupElection } from '../messages';
import { OpenElection } from '../messages/OpenElection';

jest.mock('core/objects', () => {
  const actual = jest.requireActual('core/objects');
  return {
    ...actual,
    channelFromIds: jest.fn(actual.channelFromIds),
  };
});

const mockStore = configureStore({
  reducer: combineReducers({
    ...messageReducer,
    ...electionKeyReducer,
  }),
});

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

beforeAll(() => {
  (channelFromIds as jest.Mock).mockImplementation(() => mockChannel);
});

beforeEach(() => {
  jest.clearAllMocks();
});

describe('requestCreateElection', () => {
  it('works as expected using a valid set of parameters', () => {
    requestCreateElection(
      mockLaoId,
      mockElectionNotStarted.name,
      mockElectionNotStarted.version,
      mockElectionNotStarted.start,
      mockElectionNotStarted.end,
      mockElectionNotStarted.questions,
      mockElectionNotStarted.createdAt,
    );

    expect(channelFromIds).toHaveBeenCalledWith(mockLaoId);
    expect(channelFromIds).toHaveBeenCalledTimes(1);

    const setupElectionMessage = new SetupElection(
      {
        lao: mockElectionNotStarted.lao,
        id: mockElectionNotStarted.id,
        name: mockElectionNotStarted.name,
        version: mockElectionNotStarted.version,
        created_at: mockElectionNotStarted.createdAt,
        start_time: mockElectionNotStarted.start,
        end_time: mockElectionNotStarted.end,
        questions: mockElectionNotStarted.questions,
      },
      mockLaoId,
    );

    expect(publish).toHaveBeenLastCalledWith(mockChannel, setupElectionMessage);
    expect(publish).toHaveBeenCalledTimes(1);
  });
});

describe('openElection', () => {
  it('works as expected using a valid set of parameters', () => {
    openElection(mockElectionNotStarted);

    expect(channelFromIds).toHaveBeenCalledWith(
      mockElectionNotStarted.lao,
      mockElectionNotStarted.id,
    );
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
  it('works as expected using a valid set of parameters (open ballot)', () => {
    const selectedBallots: SelectedBallots = {
      0: 0,
      1: 1,
    };

    castVote(mockElectionNotStarted, undefined, selectedBallots);

    expect(channelFromIds).toHaveBeenCalledWith(
      mockElectionNotStarted.lao,
      mockElectionNotStarted.id,
    );
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

  it('works as expected using a valid set of parameters (secret ballot)', () => {
    const selectedBallots: SelectedBallots = {
      0: 3,
      1: 7,
    };
    const keyPair = ElectionKeyPair.generate();

    castVote(mockSecretBallotElectionNotStarted, keyPair.publicKey, selectedBallots);

    expect(channelFromIds).toHaveBeenCalledWith(
      mockElectionNotStarted.lao,
      mockElectionNotStarted.id,
    );
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

    expect(castVoteMessage.votes.length).toEqual(2);
    expect(castVoteMessage.votes[0].vote).toBeString();
    expect(castVoteMessage.votes[1].vote).toBeString();

    expect(
      keyPair.privateKey
        .decrypt(new Base64UrlData(castVoteMessage.votes[0].vote as string))
        .readIntBE(0, 2),
    ).toEqual(3);
    expect(
      keyPair.privateKey
        .decrypt(new Base64UrlData(castVoteMessage.votes[1].vote as string))
        .readIntBE(0, 2),
    ).toEqual(7);

    expect(publish).toHaveBeenCalledTimes(1);
  });
});

describe('terminateElection', () => {
  it('works as expected using a valid set of parameters', () => {
    terminateElection(mockElectionOpened);

    expect(channelFromIds).toHaveBeenCalledWith(mockElectionOpened.lao, mockElectionOpened.id);
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
