import { mockAddress, mockChannel, mockKeyPair, mockPublicKey } from '__tests__/utils';
import { ProtocolError, PublicKey } from 'core/objects';

import { Greeting } from '../Greeting';

const otherAddress = 'some other address';

describe('Greeting', () => {
  describe('constructor', () => {
    it('can correctly construct a greeting instance', () => {
      const peers = [otherAddress];

      const g = new Greeting({
        address: mockAddress,
        channel: mockChannel,
        peers,
        sender: mockKeyPair.publicKey,
      });

      expect(g.address).toBe(mockAddress);
      expect(g.channel).toBe(mockChannel);
      expect(g.peers).toBe(peers);
      expect(g.sender).toBe(mockKeyPair.publicKey);
    });

    it('throw an error if the address is undefined', () => {
      const fn = () =>
        new Greeting({
          address: undefined as unknown as string,
          channel: mockChannel,
          peers: [otherAddress],
          sender: mockKeyPair.publicKey,
        });

      expect(fn).toThrow(ProtocolError);
    });

    it('throw an error if the channel is undefined', () => {
      const fn = () =>
        new Greeting({
          address: mockAddress,
          channel: undefined as unknown as string,
          peers: [otherAddress],
          sender: mockKeyPair.publicKey,
        });

      expect(fn).toThrow(ProtocolError);
    });

    it('throw an error if the peers are undefined', () => {
      const fn = () =>
        new Greeting({
          address: mockAddress,
          channel: mockChannel,
          peers: undefined as unknown as string[],
          sender: mockKeyPair.publicKey,
        });

      expect(fn).toThrow(ProtocolError);
    });

    it('throw an error if the sender is undefined', () => {
      const fn = () =>
        new Greeting({
          address: mockAddress,
          channel: mockChannel,
          peers: [otherAddress],
          sender: undefined as unknown as PublicKey,
        });

      expect(fn).toThrow(ProtocolError);
    });

    it('throw an error if the peers contain the address', () => {
      const fn = () =>
        new Greeting({
          address: mockAddress,
          channel: mockChannel,
          peers: [mockAddress],
          sender: undefined as unknown as PublicKey,
        });

      expect(fn).toThrow(ProtocolError);
    });
  });

  describe('fromJson', () => {
    it('can correctly construct a greeting instance', () => {
      const peers = [otherAddress];

      const g = Greeting.fromJson({
        address: mockAddress,
        channel: mockChannel,
        peers,
        sender: mockPublicKey,
      });

      expect(g.address).toEqual(mockAddress);
      expect(g.channel).toEqual(mockChannel);
      expect(g.peers).toEqual(peers);
      expect(g.sender.valueOf()).toEqual(mockPublicKey);
    });
  });
});
