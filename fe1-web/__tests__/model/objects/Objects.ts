/* eslint-disable */

import {
  Base64Data,
  Hash, KeyPair, KeyPairState,
  Lao, LaoState,
  PrivateKey,
  PublicKey,
  Timestamp,
  WitnessSignature
} from 'model/objects';
import { channelFromId, ROOT_CHANNEL } from 'model/objects/Channel';

const assertChai = require('chai').assert;

const mockPublicKey = new PublicKey('xjHAz+d0udy1XfHp5qugskWJVEGZETN/8DV3+ccOFSs=');
const mockSecretKey = new PrivateKey('vx0b2hbxwPBQzfPu9NdlCcYmuFjhUFuIUDx6doHRCM7GMcDP53S53LVd8enmq6CyRYlUQZkRM3/wNXf5xw4VKw==');



describe('=== Primitive objects checks ===', function() {

  describe('Base64Data', function () {
    it('should encode and decode properly', function () {
      const string: string = 'string';
      assertChai.strictEqual(Base64Data.encode(string).decode(), string);
    });
  });

  describe('Channel', function () {
    it('should return channels properly', function () {
      const id: Hash = Hash.fromStringArray("id");
      assertChai(channelFromId(), ROOT_CHANNEL);
      assertChai(channelFromId(id), `${ROOT_CHANNEL}/${id}`);
    });
  });

  describe('Hash', function () {
    it('equals work properly', function () {
      const hash1: Hash = Hash.fromStringArray("sameHash");
      const hash2: Hash = Hash.fromStringArray("sameHash");
      const hash3: Hash = Hash.fromStringArray("differentHash");

      assertChai.isTrue(hash1.equals(hash2));
      assertChai.isFalse(hash1.equals(hash3));
    });

    // FIXME add 1 more test when we settled on Hashing method
  });

  describe('PrivateKey & Signature', function () {
    it('creates signature properly', function () {
      const message: Base64Data = Base64Data.encode('message');

      assertChai.isTrue(mockSecretKey.sign(message).verify(mockPublicKey, message));
    });
  });

  describe('WitnessSignature', function () {
    it('verify works correctly', function () {
      const messageId: Hash = Hash.fromStringArray('messageId');
      const witnessSignature: WitnessSignature = new WitnessSignature({
        witness: mockPublicKey,
        signature: mockSecretKey.sign(messageId),
      });

      assertChai.isTrue(witnessSignature.verify(messageId));
    });
  });

  describe('Lao', function () {
    it('can do a state round-trip', function () {

      const laoState: LaoState = {
        id: '1234',
        name: 'MyLao',
        creation: 123,
        last_modified: 1234,
        organizer: '1234',
        witnesses: [],
      };

      const lao: Lao = Lao.fromState(laoState);

      expect(lao.toState()).toStrictEqual(laoState);
    });
  });

  describe('KeyPair', function () {
    it('can do a state round-trip', function () {

      const kpState: KeyPairState = {
        publicKey: 'public',
        privateKey: 'private',
      };

      const kp: KeyPair = KeyPair.fromState(kpState);

      expect(kp.toState()).toStrictEqual(kpState);
    });
  });

});
