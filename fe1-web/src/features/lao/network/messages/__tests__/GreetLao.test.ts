import {
  mockAddress,
  mockKeyPair,
  serializedMockLaoId,
  mockLaoId,
  mockPublicKey,
} from '__tests__/utils';
import { ActionType, ObjectType } from 'core/network/jsonrpc/messages';
import { Hash, ProtocolError, PublicKey } from 'core/objects';

import { GreetLao } from '../GreetLao';

const otherAddress = 'wss://some-other-address.com:8000/';

describe('GreetLao', () => {
  describe('constructor', () => {
    it('can correctly construct a greeting instance', () => {
      const peers = [{ address: otherAddress }];

      const g = new GreetLao({
        address: mockAddress,
        lao: mockLaoId,
        peers,
        frontend: mockKeyPair.publicKey,
      });

      expect(g.address).toEqual(mockAddress);
      expect(g.lao).toEqual(mockLaoId);
      expect(g.peers).toEqual(peers);
      expect(g.frontend).toEqual(mockKeyPair.publicKey);
    });

    it('throw an error if the address is undefined', () => {
      const fn = () =>
        new GreetLao({
          address: undefined as unknown as string,
          lao: mockLaoId,
          peers: [{ address: otherAddress }],
          frontend: mockKeyPair.publicKey,
        });

      expect(fn).toThrow(ProtocolError);
    });

    it('throw an error if the lao is undefined', () => {
      const fn = () =>
        new GreetLao({
          address: mockAddress,
          lao: undefined as unknown as Hash,
          peers: [{ address: otherAddress }],
          frontend: mockKeyPair.publicKey,
        });

      expect(fn).toThrow(ProtocolError);
    });

    it('throw an error if the peers are undefined', () => {
      const fn = () =>
        new GreetLao({
          address: mockAddress,
          lao: mockLaoId,
          peers: undefined as unknown as GreetLao['peers'],
          frontend: mockKeyPair.publicKey,
        });

      expect(fn).toThrow(ProtocolError);
    });

    it('throw an error if the frontend is undefined', () => {
      const fn = () =>
        new GreetLao({
          address: mockAddress,
          lao: mockLaoId,
          peers: [{ address: otherAddress }],
          frontend: undefined as unknown as PublicKey,
        });

      expect(fn).toThrow(ProtocolError);
    });

    it('throw an error if the peers contain the address', () => {
      const fn = () =>
        new GreetLao({
          address: mockAddress,
          lao: mockLaoId,
          peers: [{ address: mockAddress }],
          frontend: undefined as unknown as PublicKey,
        });

      expect(fn).toThrow(ProtocolError);
    });
  });

  describe('fromJson', () => {
    it('can correctly construct a greeting instance', () => {
      const peers = [{ address: otherAddress }];

      const g = GreetLao.fromJson({
        object: ObjectType.LAO,
        action: ActionType.GREET,
        address: mockAddress,
        lao: serializedMockLaoId,
        peers,
        frontend: mockPublicKey,
      });

      expect(g.address).toEqual(mockAddress);
      expect(g.lao.valueOf()).toEqual(serializedMockLaoId);
      expect(g.peers).toEqual(peers);
      expect(g.frontend.valueOf()).toEqual(mockPublicKey);
    });
  });
});
