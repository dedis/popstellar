import { publish } from 'core/network';
import { channelFromIds, Timestamp } from 'core/objects';
import { SelectedBallots } from 'features/evoting/objects';
import {
  mockElectionNotStarted,
  mockElectionOpened,
} from 'features/evoting/objects/__tests__/utils';
import 'jest-extended';
import { mockLaoIdHash } from '__tests__/utils';
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
    channelFromIds: jest.fn().mockImplementation(() => mockChannelId),
  };
});

jest.mock('core/network', () => {
  return {
    ...jest.requireActual('core/network'),
    publish: jest.fn().mockImplementation(() => Promise.resolve()),
  };
});

afterEach(() => {
  (channelFromIds as jest.Mock).mockClear();
  (publish as jest.Mock).mockClear();
});
afterAll(() => {
  (channelFromIds as jest.Mock).mockReset();
  (publish as jest.Mock).mockReset();
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

      expect((publish as jest.Mock).mock.calls[0][0]).toEqual(mockChannelId);

      expect((publish as jest.Mock).mock.calls[0][1]).toBeInstanceOf(SetupElection);
      expect(((publish as jest.Mock).mock.calls[0][1] as SetupElection).id).toEqual(
        mockElectionNotStarted.id,
      );
      expect(((publish as jest.Mock).mock.calls[0][1] as SetupElection).lao).toEqual(
        mockElectionNotStarted.lao,
      );
      expect(((publish as jest.Mock).mock.calls[0][1] as SetupElection).name).toEqual(
        mockElectionNotStarted.name,
      );
      expect(((publish as jest.Mock).mock.calls[0][1] as SetupElection).version).toEqual(
        mockElectionNotStarted.version,
      );
      expect(((publish as jest.Mock).mock.calls[0][1] as SetupElection).start_time).toEqual(
        mockElectionNotStarted.start,
      );
      expect(((publish as jest.Mock).mock.calls[0][1] as SetupElection).end_time).toEqual(
        mockElectionNotStarted.end,
      );
      expect(((publish as jest.Mock).mock.calls[0][1] as SetupElection).version).toEqual(
        mockElectionNotStarted.version,
      );
      expect(((publish as jest.Mock).mock.calls[0][1] as SetupElection).questions).toEqual(
        mockElectionNotStarted.questions,
      );
      expect(((publish as jest.Mock).mock.calls[0][1] as SetupElection).created_at).toEqual(
        mockElectionNotStarted.createdAt,
      );
      expect(publish).toHaveBeenCalledTimes(1);
    });
  });

  describe('openElection', () => {
    it('works as expected using a valid set of parameters', () => {
      openElection(mockLaoIdHash, mockElectionNotStarted);

      expect(channelFromIds).toHaveBeenCalledWith(mockLaoIdHash, mockElectionNotStarted.id);
      expect(channelFromIds).toHaveBeenCalledTimes(1);

      expect((publish as jest.Mock).mock.calls[0][0]).toEqual(mockChannelId);

      expect((publish as jest.Mock).mock.calls[0][1]).toBeInstanceOf(OpenElection);
      expect(((publish as jest.Mock).mock.calls[0][1] as OpenElection).election).toEqual(
        mockElectionNotStarted.id,
      );
      expect(((publish as jest.Mock).mock.calls[0][1] as OpenElection).lao).toEqual(
        mockElectionNotStarted.lao,
      );
      expect(
        ((publish as jest.Mock).mock.calls[0][1] as OpenElection).opened_at.valueOf(),
      ).toBeLessThanOrEqual(Timestamp.EpochNow().valueOf());

      expect(publish).toHaveBeenCalledTimes(1);
    });
  });

  describe('castVote', () => {
    it('works as expected using a valid set of parameters', () => {
      const selectedBallots: SelectedBallots = { 0: new Set([0]), 1: new Set([1]) };

      castVote(mockLaoIdHash, mockElectionNotStarted, selectedBallots);

      expect(channelFromIds).toHaveBeenCalledWith(mockLaoIdHash, mockElectionNotStarted.id);
      expect(channelFromIds).toHaveBeenCalledTimes(1);

      expect((publish as jest.Mock).mock.calls[0][0]).toEqual(mockChannelId);
      expect((publish as jest.Mock).mock.calls[0][1]).toBeInstanceOf(CastVote);
      expect(((publish as jest.Mock).mock.calls[0][1] as CastVote).election).toEqual(
        mockElectionNotStarted.id,
      );
      expect(((publish as jest.Mock).mock.calls[0][1] as CastVote).lao).toEqual(
        mockElectionNotStarted.lao,
      );
      expect(
        ((publish as jest.Mock).mock.calls[0][1] as CastVote).created_at.valueOf(),
      ).toBeLessThanOrEqual(Timestamp.EpochNow().valueOf());
      expect(((publish as jest.Mock).mock.calls[0][1] as CastVote).votes).toEqual(
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

      expect((publish as jest.Mock).mock.calls[0][0]).toEqual(mockChannelId);

      expect((publish as jest.Mock).mock.calls[0][1]).toBeInstanceOf(EndElection);
      expect(((publish as jest.Mock).mock.calls[0][1] as EndElection).election).toEqual(
        mockElectionOpened.id,
      );
      expect(((publish as jest.Mock).mock.calls[0][1] as EndElection).lao).toEqual(
        mockElectionOpened.lao,
      );
      expect(
        ((publish as jest.Mock).mock.calls[0][1] as EndElection).created_at.valueOf(),
      ).toBeLessThanOrEqual(Timestamp.EpochNow().valueOf());
      expect(((publish as jest.Mock).mock.calls[0][1] as EndElection).registered_votes).toEqual(
        EndElection.computeRegisteredVotesHash(mockElectionOpened),
      );

      expect(publish).toHaveBeenCalledTimes(1);
    });
  });
});
