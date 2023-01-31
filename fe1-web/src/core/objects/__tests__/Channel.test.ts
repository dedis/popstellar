import 'jest-extended';

import { mockLaoId } from '__tests__/utils';

import {
  channelFromIds,
  getGeneralChirpsChannel,
  getLaoIdFromChannel,
  getLastPartOfChannel,
  getReactionChannel,
  getUserSocialChannel,
} from '../Channel';
import { Hash, PublicKey } from '../index';

describe('Channel object', () => {
  it('getLaoIdFromChannel should fail on empty path', () => {
    expect(() => getLaoIdFromChannel(``)).toThrow(Error);
  });

  it('getLaoIdFromChannel should fail on root', () => {
    expect(() => getLaoIdFromChannel(`/root`)).toThrow(Error);
  });

  it('getLaoIdFromChannel should return the correct LAO ID for LAO path', () => {
    const actual = getLaoIdFromChannel(`/root/${mockLaoId}`);
    expect(actual.valueOf()).toEqual(mockLaoId.valueOf());
  });

  it('getLaoIdFromChannel should return the correct LAO ID for long paths', () => {
    const actual = getLaoIdFromChannel(`/root/${mockLaoId}/long/path`);
    expect(actual.valueOf()).toEqual(mockLaoId.valueOf());
  });

  it('channelFromIds should return the correct channel', () => {
    expect(channelFromIds()).toEqual('/root');
    expect(channelFromIds(mockLaoId)).toStrictEqual(`/root/${mockLaoId}`);
  });

  it('getUserSocialChannel should return the correct channel', () => {
    const userToken = new PublicKey('userToken');
    expect(getUserSocialChannel(mockLaoId, userToken)).toStrictEqual(
      `/root/${mockLaoId}/social/${userToken.valueOf()}`,
    );
  });

  it('getGeneralChirpsChannel should return the correct channel', () => {
    expect(getGeneralChirpsChannel(mockLaoId)).toStrictEqual(`/root/${mockLaoId}/social/chirps`);
  });

  it('getReactionChannel should return the correct channel', () => {
    expect(getReactionChannel(mockLaoId)).toStrictEqual(`/root/${mockLaoId}/social/reactions`);
  });

  it('getLastPartOfChannel works correctly', () => {
    const channel = '/root/laoID/electionID';
    const expectedHash = new Hash('electionID');
    expect(getLastPartOfChannel(channel)).toStrictEqual(expectedHash);
  });
});
