import 'jest-extended';
import '__tests__/utils/matchers';

import { curve } from '@dedis/kyber';
import Ed25519Point from '@dedis/kyber/curve/edwards25519/point';

import { Base64UrlData } from 'core/objects';
import { mockEncodedElectionKey, mockElectionKeyString } from 'features/evoting/__tests__/utils';

import { ElectionPublicKey } from '../ElectionPublicKey';

const ed25519 = curve.newCurve('edwards25519');

describe('ElectionPublicKey', () => {
  describe('constructor', () => {
    it('does not throw an error', () => {
      expect(() => new ElectionPublicKey(mockElectionKeyString)).not.toThrow();
      expect(() => new ElectionPublicKey(mockEncodedElectionKey)).not.toThrow();
    });
  });

  describe('toString()', () => {
    it('converts the key to a string', () => {
      const key = new ElectionPublicKey(mockEncodedElectionKey);
      expect(key.toString()).toBeString();
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
      const p = ed25519.point().pick() as Ed25519Point;
      const key2 = new ElectionPublicKey(new Base64UrlData(p.toProto().toString('base64')));
      expect(key1.equals(key2)).toBeFalse();
    });
  });

  describe('encrypt()', () => {
    it('produces output of the correct form', () => {
      const key = new ElectionPublicKey(mockEncodedElectionKey);
      const encryptedData = key.encrypt(Buffer.from('x', 'utf-8'));
      const b = Buffer.from(encryptedData, 'base64');

      expect(Buffer.byteLength(b)).toEqual(64);

      expect(() => {
        ed25519.point().unmarshalBinary(b.slice(0, 32));
        ed25519.point().unmarshalBinary(b.slice(32, 64));
      }).not.toThrow();
    });
  });

  it('Decoding reconstructs the correct Ed25519 point', () => {
    const p = ed25519.point().pick() as Ed25519Point;
    const electionKey = new ElectionPublicKey(new Base64UrlData(p.toProto().toString('base64')));

    expect(electionKey.point.equals(p)).toBeTrue();
  });
});
