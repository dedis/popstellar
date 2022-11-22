import 'jest-extended';
import '__tests__/utils/matchers';

import { curve } from '@dedis/kyber';

import { Base64UrlData } from 'core/objects';
import {
  mockEncodedElectionKey,
  mockElectionKeyState,
  mockElectionKey,
} from 'features/evoting/__tests__/utils';

import { ElectionPublicKey } from '../ElectionPublicKey';

const ed25519 = curve.newCurve('edwards25519');

describe('ElectionPublicKey', () => {
  describe('constructor', () => {
    it('does not throw an error', () => {
      expect(() => new ElectionPublicKey(new Base64UrlData(mockElectionKeyState))).not.toThrow();
      expect(() => new ElectionPublicKey(mockEncodedElectionKey)).not.toThrow();
    });
  });

  describe('toString()', () => {
    it('converts the key to a string', () => {
      const key = new ElectionPublicKey(mockEncodedElectionKey);
      expect(key.toString()).toBeString();
    });
  });

  describe('toState()', () => {
    it('converts the key to a string', () => {
      const key = new ElectionPublicKey(mockEncodedElectionKey);
      expect(key.toState()).toBeString();
    });
  });

  describe('fromState()', () => {
    it('converts from the state representation to a class instance', () => {
      const key = ElectionPublicKey.fromState(mockElectionKeyState);
      expect(key).toBeInstanceOf(ElectionPublicKey);
    });

    it('equals the instance .toState() was called on', () => {
      const state = mockElectionKey.toState();
      expect(ElectionPublicKey.fromState(state).equals(mockElectionKey)).toBeTrue();
    });
  });

  describe('toBase64()', () => {
    it('converts the key to a base64 string', () => {
      const key = new ElectionPublicKey(mockEncodedElectionKey);
      expect(key.toBase64()).toBeBase64Url();
    });
  });

  describe('equals()', () => {
    it('returns true if the point is the same', () => {
      const key1 = new ElectionPublicKey(mockEncodedElectionKey);
      const key2 = new ElectionPublicKey(mockEncodedElectionKey);
      expect(key1.equals(key2)).toBeTrue();
    });

    it('returns false if the point is different', () => {
      const key1 = new ElectionPublicKey(mockEncodedElectionKey);
      const p = ed25519.point().pick();
      const key2 = new ElectionPublicKey(Base64UrlData.encode(p.marshalBinary()));
      expect(key1.equals(key2)).toBeFalse();
    });
  });

  describe('encrypt()', () => {
    it('produces output of the correct form', () => {
      const key = new ElectionPublicKey(mockEncodedElectionKey);
      const encryptedData = key.encrypt(Buffer.from('x', 'utf-8'));
      const b = encryptedData.toBuffer();

      expect(Buffer.byteLength(b)).toEqual(64);

      expect(() => {
        ed25519.point().unmarshalBinary(b.slice(0, 32));
        ed25519.point().unmarshalBinary(b.slice(32, 64));
      }).not.toThrow();
    });
  });

  it('Decoding reconstructs the correct Ed25519 point', () => {
    const p = ed25519.point().pick();
    const electionKey = new ElectionPublicKey(Base64UrlData.encode(p.marshalBinary()));

    expect(electionKey.point.equals(p)).toBeTrue();
  });
});
