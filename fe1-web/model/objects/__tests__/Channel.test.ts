import 'jest-extended';

import { KeyPairStore } from 'store';
import { Hash } from '../Hash';
import {
  channelFromIds,
  generalChirpsChannel,
  getLastChannel,
  userSocialChannel,
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

  it('userSocialChannel should return the correct channel', () => {
    const keyPair = KeyPair.fromState({
      publicKey: PUBLIC_KEY,
      privateKey: PRIVATE_KEY,
    });
    KeyPairStore.store(keyPair);
    const pk = new PublicKey(PUBLIC_KEY);
    expect(userSocialChannel(FAKE_ID)).toStrictEqual(`/root/${FAKE_ID}/social/${pk}`);
  });

  it('generalChirpsChannel should return the correct channel', () => {
    expect(generalChirpsChannel(FAKE_ID)).toStrictEqual(`/root/${FAKE_ID}/social/chirps`);
  });

  it('getLastChannel works correctly', () => {
    const channel = '/root/laoID/electionID';
    const expectedHash = new Hash('electionID');
    expect(getLastChannel(channel)).toStrictEqual(expectedHash);
  });
});
