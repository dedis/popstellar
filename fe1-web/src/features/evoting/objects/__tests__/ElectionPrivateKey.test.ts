import 'jest-extended';
import '__tests__/utils/matchers';

import { curve } from '@dedis/kyber';
import Ed25519Scalar from '@dedis/kyber/curve/edwards25519/scalar';

import { Base64UrlData } from 'core/objects';
import { mockEncodedElectionPrivateKey } from 'features/evoting/__tests__/utils';

import { ElectionPrivateKey } from '../ElectionPrivateKey';

const ed25519 = curve.newCurve('edwards25519');

describe('ElectionPrivateKey', () => {
  describe('constructor', () => {
    it('does not throw an error', () => {
      expect(() => new ElectionPrivateKey(mockEncodedElectionPrivateKey)).not.toThrow();
      expect(() => new ElectionPrivateKey(mockEncodedElectionPrivateKey)).not.toThrow();
    });
  });

  describe('toString()', () => {
    it('converts the key to a string', () => {
      const key = new ElectionPrivateKey(mockEncodedElectionPrivateKey);
      expect(key.toString()).toBeString();
    });
  });

  describe('toBase64()', () => {
    it('converts the key to a base64 string', () => {
      const key = new ElectionPrivateKey(mockEncodedElectionPrivateKey);
      expect(key.toBase64()).toBeBase64Url();
    });
  });

  describe('equals()', () => {
    it('returns true if the scalar is the same', () => {
      const key1 = new ElectionPrivateKey(mockEncodedElectionPrivateKey);
      const key2 = new ElectionPrivateKey(mockEncodedElectionPrivateKey);
      expect(key1.equals(key2)).toBeTrue();
    });

    it('returns false if the scalar is different', () => {
      const key1 = new ElectionPrivateKey(mockEncodedElectionPrivateKey);
      const p = ed25519.scalar().pick() as Ed25519Scalar;
      const key2 = new ElectionPrivateKey(Base64UrlData.encode(p.marshalBinary()));
      expect(key1.equals(key2)).toBeFalse();
    });
  });

  it('Decoding reconstructs the correct Ed25519 scalar', () => {
    const p = ed25519.scalar().pick() as Ed25519Scalar;
    const electionKey = new ElectionPrivateKey(Base64UrlData.encode(p.marshalBinary()));

    expect(electionKey.scalar.equals(p)).toBeTrue();
  });
});
