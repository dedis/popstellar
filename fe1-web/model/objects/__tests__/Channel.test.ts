import 'jest-extended';

import { KeyPairStore } from 'store';
import { Hash } from '../Hash';
import {
  channelFromIds,
  getGeneralChirpsChannel,
  getLastPartOfChannel,
  getCurrentUserSocialChannel,
  getUserSocialChannel,
} from '../Channel';
import { KeyPair } from '../KeyPair';
import { PublicKey } from '../PublicKey';

const FAKE_ID = Hash.fromStringArray('id');
const PUBLIC_KEY = '1234';
const PRIVATE_KEY = '5678';

describe('Channel object', () => {
  it('channelFromIds should should return the correct channel', () => {
    expect(channelFromIds()).toEqual('/root');
    expect(channelFromIds(FAKE_ID)).toStrictEqual(`/root/${FAKE_ID}`);
  });

  it('getCurrentUserSocialChannel should return the correct channel', () => {
    const keyPair = KeyPair.fromState({
      publicKey: PUBLIC_KEY,
      privateKey: PRIVATE_KEY,
    });
    KeyPairStore.store(keyPair);
    const pk = new PublicKey(PUBLIC_KEY);
    expect(getCurrentUserSocialChannel(FAKE_ID))
      .toStrictEqual(`/root/${FAKE_ID}/social/${pk}`);
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
