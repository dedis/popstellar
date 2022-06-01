import 'jest-extended';

import { mockLaoIdHash } from '__tests__/utils';
import { publish } from 'core/network';
import { channelFromIds, Timestamp } from 'core/objects';
import { mockElectionNotStarted, mockElectionOpened } from 'features/evoting/__tests__/utils';
import { SelectedBallots } from 'features/evoting/objects';

import {
  castVote,
  openElection,
  requestCreateElection,
  terminateElection,
} from '../ElectionMessageApi';
import { CastVote, EndElection, SetupElection } from '../messages';
import { OpenElection } from '../messages/OpenElection';

const mockChannelId = 'some channel id';

jest.mock('core/objects', () => {
  return {
    ...jest.requireActual('core/objects'),
    channelFromIds: jest.fn(() => mockChannelId),
  };
});

jest.mock('core/network');

afterEach(() => {
  jest.clearAllMocks();
});

describe('mockElectionNotStarted.id', () => {
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
      );

      expect(channelFromIds).toHaveBeenCalledWith(mockLaoIdHash);
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
        mockLaoIdHash,
      );

      expect(publish).toHaveBeenLastCalledWith(mockChannelId, setupElectionMessage);
      expect(publish).toHaveBeenCalledTimes(1);
    });
  });

  describe('openElection', () => {
    it('works as expected using a valid set of parameters', () => {
      openElection(mockLaoIdHash, mockElectionNotStarted);

      expect(channelFromIds).toHaveBeenCalledWith(mockLaoIdHash, mockElectionNotStarted.id);
      expect(channelFromIds).toHaveBeenCalledTimes(1);

      // cannot directly match the openElection message here as openedAt is set inside the openElection function
      expect(publish).toHaveBeenCalledWith(mockChannelId, expect.anything());

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
      expect(publish).toHaveBeenCalledWith(mockChannelId, expect.anything());

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
      expect(publish).toHaveBeenCalledWith(mockChannelId, expect.anything());

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
});
