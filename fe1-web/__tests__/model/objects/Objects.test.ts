import 'jest-extended';

import {
  Base64Data,
  Hash, KeyPair, KeyPairState,
  Lao, LaoState,
  PrivateKey,
  PublicKey,
  WitnessSignature,
} from 'model/objects';
import { channelFromId } from 'model/objects/Channel';

// @ts-ignore
import testKeyPair from '__tests__/../../tests/data/keypair.json';

const mockPublicKey = new PublicKey(testKeyPair.publicKey);
const mockSecretKey = new PrivateKey(testKeyPair.privateKey);

describe('=== Primitive objects checks ===', () => {
  describe('Base64Data', () => {
    it('should encode and decode properly', () => {
      const string: string = 'string';
      expect(Base64Data.encode(string).decode()).toBe(string);
    });
  });

  describe('Channel', () => {
    it('should return channels properly', () => {
      const id: Hash = Hash.fromStringArray('id');

      expect(channelFromId()).toBeTruthy();
      expect(channelFromId(id)).toBeTruthy();
    });
  });

  describe('Hash', () => {
    it('equals work properly', () => {
      const hash1: Hash = Hash.fromStringArray('sameHash');
      const hash2: Hash = Hash.fromStringArray('sameHash');
      const hash3: Hash = Hash.fromStringArray('differentHash');

      expect(hash1.equals(hash2)).toBe(true);
      expect(hash1.equals(hash3)).toBe(false);
    });

    // FIXME add 1 more test when we settled on Hashing method
  });

  describe('PrivateKey & Signature', () => {
    it('creates signature properly', () => {
      const message: Base64Data = Base64Data.encode('message');

      expect(mockSecretKey.sign(message).verify(mockPublicKey, message)).toBe(true);
    });
  });

  describe('WitnessSignature', () => {
    it('verify works correctly', () => {
      const messageId: Hash = Hash.fromStringArray('messageId');
      const witnessSignature: WitnessSignature = new WitnessSignature({
        witness: mockPublicKey,
        signature: mockSecretKey.sign(messageId),
      });

      expect(witnessSignature.verify(messageId)).toBe(true);
    });
  });

  describe('Lao', () => {
    it('can do a state round-trip', () => {
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

  describe('KeyPair', () => {
    it('can do a state round-trip', () => {
      const kpState: KeyPairState = {
        publicKey: 'public',
        privateKey: 'private',
      };

      const kp: KeyPair = KeyPair.fromState(kpState);

      expect(kp.toState()).toStrictEqual(kpState);
    });
  });
});
