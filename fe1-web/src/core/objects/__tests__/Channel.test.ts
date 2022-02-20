import 'jest-extended';

import { mockLaoIdHash } from '__tests__/utils/TestUtils';
import { Hash, PublicKey } from '..';
import {
  channelFromIds,
  getGeneralChirpsChannel,
  getLastPartOfChannel,
  getReactionChannel,
  getUserSocialChannel,
} from '../Channel';

describe('Channel object', () => {
  it('channelFromIds should should return the correct channel', () => {
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
