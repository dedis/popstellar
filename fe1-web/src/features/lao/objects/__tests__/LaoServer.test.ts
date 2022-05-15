import {
  mockAddress,
  mockKeyPair,
  mockLaoId,
  mockLaoIdHash,
  mockPopToken,
  mockPublicKey,
  mockPublicKey2,
} from '__tests__/utils';
import { PublicKey } from 'core/objects/PublicKey';

import { LaoServer } from '../LaoServer';

describe('Server', () => {
  describe('constructor', () => {
    it('correctly constructs a server object', () => {
      const s = new LaoServer({
        laoId: mockLaoIdHash,
        address: mockAddress,
        serverPublicKey: mockKeyPair.publicKey,
        frontendPublicKey: mockPopToken.publicKey,
      });

      expect(s.laoId.valueOf()).toEqual(mockLaoId);
      expect(s.address).toEqual(mockAddress);
      expect(s.serverPublicKey).toEqual(mockKeyPair.publicKey);
    });

    it('throws an error if the address is undefined', () => {
      const fn = () =>
        new LaoServer({
          laoId: mockLaoIdHash,
          address: undefined as unknown as string,
          serverPublicKey: mockKeyPair.publicKey,
          frontendPublicKey: mockPopToken.publicKey,
        });

      expect(fn).toThrow(Error);
    });

    it('throws an error if the server public key is undefined', () => {
      const fn = () =>
        new LaoServer({
          laoId: mockLaoIdHash,
          address: mockAddress,
          serverPublicKey: undefined as unknown as PublicKey,
          frontendPublicKey: mockPopToken.publicKey,
        });

      expect(fn).toThrow(Error);
    });

    it('throws an error if the frontend key is undefined', () => {
      const fn = () =>
        new LaoServer({
          laoId: mockLaoIdHash,
          address: mockAddress,
          serverPublicKey: mockKeyPair.publicKey,
          frontendPublicKey: undefined as unknown as PublicKey,
        });

      expect(fn).toThrow(Error);
    });
  });

  describe('fromState', () => {
    it('correctly constructs a server object from a server state', () => {
      const s = LaoServer.fromState({
        laoId: mockLaoId,
        address: mockAddress,
        serverPublicKey: mockPublicKey,
        frontendPublicKey: mockPublicKey2,
      });

      expect(s.laoId.valueOf()).toEqual(mockLaoId);
      expect(s.address).toEqual(mockAddress);
      expect(s.serverPublicKey.valueOf()).toEqual(mockKeyPair.publicKey.valueOf());
    });
  });

  describe('toState', () => {
    it('correctly constructs a server state object from a server instance', () => {
      const s = new LaoServer({
        laoId: mockLaoIdHash,
        address: mockAddress,
        serverPublicKey: mockKeyPair.publicKey,
        frontendPublicKey: mockPopToken.publicKey,
      }).toState();

      expect(s.laoId).toEqual(mockLaoId);
      expect(s.address).toEqual(mockAddress);
      expect(s.serverPublicKey).toEqual(mockPublicKey);
      expect(s.frontendPublicKey).toEqual(mockPublicKey2);
    });
  });
});
