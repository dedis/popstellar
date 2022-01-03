import 'jest-extended';

import { Hash } from '../Hash';
import {
  channelFromIds,
  getGeneralChirpsChannel,
  getLastPartOfChannel,
  getUserSocialChannel,
} from '../Channel';
import { PublicKey } from '../PublicKey';

const FAKE_ID = Hash.fromStringArray('id');

describe('Channel object', () => {
  it('channelFromIds should should return the correct channel', () => {
    expect(channelFromIds()).toEqual('/root');
    expect(channelFromIds(FAKE_ID)).toStrictEqual(`/root/${FAKE_ID}`);
  });

  it('getUserSocialChannel should return the correct channel', () => {
    const userToken = new PublicKey('userToken');
    expect(getUserSocialChannel(FAKE_ID, userToken))
      .toStrictEqual(`/root/${FAKE_ID}/social/${userToken.valueOf()}`);
  });

  it('getGeneralChirpsChannel should return the correct channel', () => {
    expect(getGeneralChirpsChannel(FAKE_ID))
      .toStrictEqual(`/root/${FAKE_ID}/social/chirps`);
  });

  it('getLastPartOfChannel works correctly', () => {
    const channel = '/root/laoID/electionID';
    const expectedHash = new Hash('electionID');
    expect(getLastPartOfChannel(channel)).toStrictEqual(expectedHash);
  });
});
