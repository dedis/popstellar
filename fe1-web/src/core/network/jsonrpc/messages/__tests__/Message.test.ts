import 'jest-extended';

import {
  configureTestFeatures,
  mockChannel,
  mockKeyPair,
  mockLao,
  mockLaoId,
  mockPopToken,
} from '__tests__/utils';
import { Base64UrlData, EventTags, Hash, Timestamp } from 'core/objects';
import { EndElection } from 'features/evoting/network/messages';
import { AddChirp } from 'features/social/network/messages/chirp';

import { encodeMessageData, Message } from '../index';

jest.mock('features/wallet/objects/Token.ts', () => ({
  getCurrentPopTokenFromStore: jest.fn(() => Promise.resolve(mockPopToken)),
}));

beforeAll(configureTestFeatures);

describe('Message', () => {
  it('fromData signs the message correctly when adding a chirp', async () => {
    const messageData = new AddChirp({
      text: 'text',
      timestamp: new Timestamp(1607277600),
    });

    const m = Message.fromData(messageData, mockPopToken, mockChannel);

    const encodedDataJson: Base64UrlData = encodeMessageData(messageData);
    const signature = mockPopToken.privateKey.sign(encodedDataJson);
    expect(m.sender).toEqual(mockPopToken.publicKey);
    expect(m.signature).toEqual(signature);
  });

  it('fromData signs the message correctly when ending an election', async () => {
    const messageData = new EndElection({
      lao: mockLao.id,
      election: Hash.fromStringArray(
        EventTags.ELECTION,
        mockLaoId.serialize(),
        '5678',
        '1607277600',
      ),
      created_at: new Timestamp(1607277600),
      registered_votes: new Hash('1234'),
    });

    const m = Message.fromData(messageData, mockKeyPair, mockChannel);

    const encodedDataJson: Base64UrlData = encodeMessageData(messageData);
    const signature = mockKeyPair.privateKey.sign(encodedDataJson);
    expect(m.sender).toEqual(mockKeyPair.publicKey);
    expect(m.signature).toEqual(signature);
  });
});
