import 'jest-extended';

import { Hash } from '../Hash';
import {
  channelFromIds,
  generalChirpsChannel,
  getLastChannel,
} from '../Channel';

const FAKE_ID = Hash.fromStringArray('id');

describe('Channel object', () => {
  it('channelFromIds should should return the correct channel', () => {
    expect(channelFromIds()).toEqual('/root');
    expect(channelFromIds(FAKE_ID)).toStrictEqual(`/root/${FAKE_ID}`);
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
