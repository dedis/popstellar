import 'jest-extended';
import { mockLaoIdHash } from '__tests__/utils';

import { Hash, PublicKey } from '../index';
import {
  getLaoIdFromChannel,
  channelFromIds,
  getGeneralChirpsChannel,
  getLastPartOfChannel,
  getReactionChannel,
  getUserSocialChannel,
} from '../Channel';

describe('Channel object', () => {
  it('getLaoIdFromChannel should fail on empty path', () => {
    expect(() => getLaoIdFromChannel(``)).toThrow(Error);
  });

  it('getLaoIdFromChannel should fail on root', () => {
    expect(() => getLaoIdFromChannel(`/root`)).toThrow(Error);
  });

  it('getLaoIdFromChannel should return the correct LAO ID for LAO path', () => {
    const actual = getLaoIdFromChannel(`/root/${mockLaoIdHash}`);
    expect(actual.valueOf()).toEqual(mockLaoIdHash.valueOf());
  });

  it('getLaoIdFromChannel should return the correct LAO ID for long paths', () => {
    const actual = getLaoIdFromChannel(`/root/${mockLaoIdHash}/long/path`);
    expect(actual.valueOf()).toEqual(mockLaoIdHash.valueOf());
  });

  it('channelFromIds should return the correct channel', () => {
    expect(channelFromIds()).toEqual('/root');
    expect(channelFromIds(mockLaoIdHash)).toStrictEqual(`/root/${mockLaoIdHash}`);
  });

  it('getUserSocialChannel should return the correct channel', () => {
    const userToken = new PublicKey('userToken');
    expect(getUserSocialChannel(mockLaoIdHash, userToken)).toStrictEqual(
      `/root/${mockLaoIdHash}/social/${userToken.valueOf()}`,
    );
  });

  it('getGeneralChirpsChannel should return the correct channel', () => {
    expect(getGeneralChirpsChannel(mockLaoIdHash)).toStrictEqual(
      `/root/${mockLaoIdHash}/social/chirps`,
    );
  });

  it('getReactionChannel should return the correct channel', () => {
    expect(getReactionChannel(mockLaoIdHash)).toStrictEqual(
      `/root/${mockLaoIdHash}/social/reactions`,
    );
  });

  it('getLastPartOfChannel works correctly', () => {
    const channel = '/root/laoID/electionID';
    const expectedHash = new Hash('electionID');
    expect(getLastPartOfChannel(channel)).toStrictEqual(expectedHash);
  });
});
