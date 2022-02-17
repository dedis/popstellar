import 'jest-extended';

import '__tests__/utils/matchers';
import { Hash, PublicKey, Timestamp } from 'model/objects';

import { Reaction, ReactionState } from '../Reaction';

const TIMESTAMP = new Timestamp(12345);
const PK = new PublicKey('publicKey');
const CHIRP_ID = new Hash('chirp_id');
const ID = new Hash('id');

describe('Reaction object', () => {
  it('does a state round trip correctly', () => {
    const reactionState: ReactionState = {
      id: '1234',
      sender: 'me',
      codepoint: '👍',
      chirpId: '5678',
      time: 1234,
    };
    const reaction = Reaction.fromState(reactionState);
    expect(reaction.toState()).toStrictEqual(reactionState);
  });

  describe('constructor', () => {
    it('throws an error when object is undefined', () => {
      const partial = undefined as unknown as Partial<Reaction>;
      const wrongReaction = () => new Reaction(partial);
      expect(wrongReaction).toThrow(Error);
    });

    it('throws an error when object is null', () => {
      const partial = null as unknown as Partial<Reaction>;
      const wrongReaction = () => new Reaction(partial);
      expect(wrongReaction).toThrow(Error);
    });

    it('throws an error when id is undefined', () => {
      const wrongReaction = () => new Reaction({
        sender: PK,
        codepoint: '👍',
        chirpId: CHIRP_ID,
        time: TIMESTAMP,
      });
      expect(wrongReaction).toThrow(Error);
    });

    it('throws an error when sender is undefined', () => {
      const wrongReaction = () => new Reaction({
        id: ID,
        codepoint: '👍',
        chirpId: CHIRP_ID,
        time: TIMESTAMP,
      });
      expect(wrongReaction).toThrow(Error);
    });

    it('throws an error when codepoint is undefined', () => {
      const wrongReaction = () => new Reaction({
        id: ID,
        sender: PK,
        chirpId: CHIRP_ID,
        time: TIMESTAMP,
      });
      expect(wrongReaction).toThrow(Error);
    });

    it('throws an error when chirp_id is undefined', () => {
      const wrongReaction = () => new Reaction({
        id: ID,
        sender: PK,
        codepoint: '👍',
        time: TIMESTAMP,
      });
      expect(wrongReaction).toThrow(Error);
    });

    it('throws an error when time is undefined', () => {
      const wrongReaction = () => new Reaction({
        id: ID,
        sender: PK,
        codepoint: '👍',
        chirpId: CHIRP_ID,
      });
      expect(wrongReaction).toThrow(Error);
    });
  });
});
