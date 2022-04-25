import { mockAddress, mockKeyPair, mockLaoId, mockLaoIdHash, mockPublicKey } from '__tests__/utils';
import { ActionType, ObjectType } from 'core/network/jsonrpc/messages';
import { Hash, ProtocolError, PublicKey } from 'core/objects';

import { GreetLao } from '../GreetLao';

const otherAddress = 'wss://some-other-address.com';

describe('GreetLao', () => {
  describe('constructor', () => {
    it('can correctly construct a greeting instance', () => {
      const peers = [{ address: otherAddress }];

      const g = new GreetLao({
        address: mockAddress,
        lao: mockLaoIdHash,
        peers,
        sender: mockKeyPair.publicKey,
      });

      expect(g.address).toBe(mockAddress);
      expect(g.lao).toBe(mockLaoIdHash);
      expect(g.peers).toBe(peers);
      expect(g.sender).toBe(mockKeyPair.publicKey);
    });

    it('throw an error if the address is undefined', () => {
      const fn = () =>
        new GreetLao({
          address: undefined as unknown as string,
          lao: mockLaoIdHash,
          peers: [{ address: otherAddress }],
          sender: mockKeyPair.publicKey,
        });

      expect(fn).toThrow(ProtocolError);
    });

    it('throw an error if the lao is undefined', () => {
      const fn = () =>
        new GreetLao({
          address: mockAddress,
          lao: undefined as unknown as Hash,
          peers: [{ address: otherAddress }],
          sender: mockKeyPair.publicKey,
        });

      expect(fn).toThrow(ProtocolError);
    });

    it('throw an error if the peers are undefined', () => {
      const fn = () =>
        new GreetLao({
          address: mockAddress,
          lao: mockLaoIdHash,
          peers: undefined as unknown as GreetLao['peers'],
          sender: mockKeyPair.publicKey,
        });

      expect(fn).toThrow(ProtocolError);
    });

    it('throw an error if the sender is undefined', () => {
      const fn = () =>
        new GreetLao({
          address: mockAddress,
          lao: mockLaoIdHash,
          peers: [{ address: otherAddress }],
          sender: undefined as unknown as PublicKey,
        });

      expect(fn).toThrow(ProtocolError);
    });

    it('throw an error if the peers contain the address', () => {
      const fn = () =>
        new GreetLao({
          address: mockAddress,
          lao: mockLaoIdHash,
          peers: [{ address: mockAddress }],
          sender: undefined as unknown as PublicKey,
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
        lao: mockLaoId,
        peers,
        sender: mockPublicKey,
      });

      expect(g.address).toEqual(mockAddress);
      expect(g.lao.valueOf()).toEqual(mockLaoId);
      expect(g.peers).toEqual(peers);
      expect(g.sender.valueOf()).toEqual(mockPublicKey);
    });
  });
});
