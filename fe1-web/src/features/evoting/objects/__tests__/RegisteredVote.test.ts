import 'jest-extended';
import '__tests__/utils/matchers';

import { Hash, PublicKey, Timestamp } from 'core/objects';
import { OmitMethods } from 'core/types';

import { RegisteredVote, Vote } from '../index';

const vote1 = new Vote({
  id: new Hash('v1'),
  question: new Hash('q1'),
  vote: 0,
});

const mockMessageId = new Hash('messageId1');

const registeredVotes = new RegisteredVote({
  createdAt: new Timestamp(1520255700),
  sender: new PublicKey('Sender1'),
  votes: [vote1],
  messageId: mockMessageId,
});

describe('RegisteredVote', () => {
  it('does a state round trip correctly', () => {
    const e = RegisteredVote.fromState(registeredVotes.toState());
    expect(e).toEqual(registeredVotes);
  });

  describe('constructor', () => {
    it('throws an error when object is undefined', () => {
      const partial = undefined as unknown as OmitMethods<RegisteredVote>;
      const createWrongRegisteredVote = () => new RegisteredVote(partial);
      expect(createWrongRegisteredVote).toThrow(Error);
    });

    it("throws an error when 'messageId' is undefined", () => {
      const createWrongRegisteredVote = () =>
        new RegisteredVote({
          createdAt: new Timestamp(1520255700),
          sender: new PublicKey('Sender1'),
          votes: [vote1],
          messageId: undefined as unknown as Hash,
        });
      expect(createWrongRegisteredVote).toThrow(Error);
    });

    it("throws an error when 'sender' is undefined", () => {
      const createWrongRegisteredVote = () =>
        new RegisteredVote({
          createdAt: new Timestamp(1520255700),
          sender: undefined as unknown as PublicKey,
          votes: [vote1],
          messageId: mockMessageId,
        });
      expect(createWrongRegisteredVote).toThrow(Error);
    });

    it("throws an error when 'votes' is undefined", () => {
      const createWrongRegisteredVote = () =>
        new RegisteredVote({
          createdAt: new Timestamp(1520255700),
          sender: new PublicKey('Sender1'),
          votes: undefined as unknown as Vote[],
          messageId: mockMessageId,
        });
      expect(createWrongRegisteredVote).toThrow(Error);
    });

    it("throws an error when 'createdAt' is undefined", () => {
      const createWrongRegisteredVote = () =>
        new RegisteredVote({
          createdAt: undefined as unknown as Timestamp,
          sender: new PublicKey('Sender1'),
          votes: [vote1],
          messageId: mockMessageId,
        });
      expect(createWrongRegisteredVote).toThrow(Error);
    });
  });
});
