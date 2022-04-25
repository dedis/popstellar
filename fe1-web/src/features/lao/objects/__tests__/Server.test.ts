import { mockAddress, mockKeyPair, mockLaoId, mockLaoIdHash, mockPublicKey } from '__tests__/utils';
import { PublicKey } from 'core/objects/PublicKey';

import { Server } from '../Server';

describe('Server', () => {
  describe('constructor', () => {
    it('correctly constructs a server object', () => {
      const s = new Server({
        laoId: mockLaoIdHash,
        address: mockAddress,
        publicKey: mockKeyPair.publicKey,
      });

      expect(s.laoId.valueOf()).toBe(mockLaoId);
      expect(s.address).toBe(mockAddress);
      expect(s.publicKey).toBe(mockKeyPair.publicKey);
    });

    it('throws an error of the address is undefined', () => {
      const fn = () =>
        new Server({
          laoId: mockLaoIdHash,
          address: undefined as unknown as string,
          publicKey: mockKeyPair.publicKey,
        });

      expect(fn).toThrow(Error);
    });

    it('throws an error of the public key is undefined', () => {
      const fn = () =>
        new Server({
          laoId: mockLaoIdHash,
          address: mockAddress,
          publicKey: undefined as unknown as PublicKey,
        });

      expect(fn).toThrow(Error);
    });
  });

  describe('fromState', () => {
    it('correctly constructs a server object from a server state', () => {
      const s = Server.fromState({
        laoId: mockLaoId,
        address: mockAddress,
        publicKey: mockPublicKey,
      });

      expect(s.laoId.valueOf()).toEqual(mockLaoId);
      expect(s.address).toEqual(mockAddress);
      expect(s.publicKey.valueOf()).toEqual(mockKeyPair.publicKey.valueOf());
    });
  });

  describe('toState', () => {
    it('correctly constructs a server state object from a server instance', () => {
      const s = new Server({
        laoId: mockLaoIdHash,
        address: mockAddress,
        publicKey: mockKeyPair.publicKey,
      }).toState();

      expect(s.laoId).toEqual(mockLaoId);
      expect(s.address).toEqual(mockAddress);
      expect(s.publicKey).toEqual(mockPublicKey);
    });
  });
});
