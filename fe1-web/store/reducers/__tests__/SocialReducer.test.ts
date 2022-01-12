import 'jest-extended';
import { AnyAction } from 'redux';
import keyPair from 'test_data/keypair.json';
import { Hash, PublicKey, Timestamp } from 'model/objects';
import { Chirp } from 'model/objects/Chirp';
import { describe } from '@jest/globals';
import { Reaction } from 'model/objects/Reaction';
import {
  socialReduce,
  addChirp,
  makeChirpsList,
  deleteChirp,
  addReaction,
  makeReactionsList,
} from '../SocialReducer';

const mockPublicKey = new PublicKey(keyPair.publicKey);
const org = mockPublicKey;
const name = 'MyLao';
const mockLaoIdHash: Hash = Hash.fromStringArray(
  org.toString(), new Timestamp(160000000).toString(), name,
);
const mockLaoId: string = mockLaoIdHash.toString();
const mockSender1: PublicKey = new PublicKey('Douglas Adams');
const mockSender2: PublicKey = new PublicKey('Gandalf');
const mockChirpId1: Hash = Hash.fromString('1234');
const mockChirpId2: Hash = Hash.fromString('5678');
const mockTimestamp: Timestamp = new Timestamp(1606666600);

const chirp1 = new Chirp({
  id: mockChirpId1,
  sender: mockSender1,
  text: 'Don\'t panic.',
  time: new Timestamp(1605555500),
  isDeleted: false,
}).toState();

const chirp1Deleted = new Chirp({
  id: Hash.fromString('1234'),
  sender: new PublicKey('Douglas Adams'),
  text: '',
  time: new Timestamp(1605555500),
  isDeleted: true,
}).toState();

const chirp0 = new Chirp({
  id: Hash.fromString('000'),
  sender: new PublicKey('Joker'),
  text: '',
  time: new Timestamp(1605555500),
  isDeleted: true,
}).toState();

const chirp2 = new Chirp({
  id: mockChirpId2,
  sender: mockSender2,
  text: 'You shall not pass! You shall not pass! You shall not pass! You shall not pass! You shall not pass! You shall not pass!',
  time: new Timestamp(1607777700),
}).toState();

const chirp3 = new Chirp({
  id: Hash.fromString('12345'),
  sender: mockSender1,
  text: 'Time is an illusion',
  time: mockTimestamp,
}).toState();

const chirp4 = new Chirp({
  id: Hash.fromString('123456'),
  sender: mockSender1,
  text: 'The answer is 42',
  time: new Timestamp(1608888800),
}).toState();

const reaction1 = new Reaction({
  id: Hash.fromString('1111'),
  sender: mockSender1,
  codepoint: '👍',
  chirp_id: mockChirpId1,
  time: mockTimestamp,
}).toState();

const reaction2 = new Reaction({
  id: Hash.fromString('2222'),
  sender: mockSender1,
  codepoint: '❤️',
  chirp_id: mockChirpId1,
  time: mockTimestamp,
}).toState();

const reaction3 = new Reaction({
  id: Hash.fromString('3333'),
  sender: mockSender2,
  codepoint: '👍',
  chirp_id: mockChirpId1,
  time: mockTimestamp,
}).toState();

const reaction4 = new Reaction({
  id: Hash.fromString('4444'),
  sender: mockSender2,
  codepoint: '👍',
  chirp_id: mockChirpId2,
  time: mockTimestamp,
}).toState();

const emptyState = {
  byLaoId: {
    myLaoId: {
      allIdsInOrder: [],
      byId: {},
      byUser: {},
      reactionsByChirp: {},
    },
  },
};

const chirpFilledState1 = {
  byLaoId: {
    myLaoId: {
      allIdsInOrder: [],
      byId: {},
      byUser: {},
      reactionsByChirp: {},
    },
    [mockLaoId]: {
      allIdsInOrder: [chirp1.id],
      byId: { [chirp1.id]: chirp1 },
      byUser: { [chirp1.sender]: [chirp1.id] },
      reactionsByChirp: {},
    },
  },
};

const chirpFilledState2 = {
  byLaoId: {
    myLaoId: {
      allIdsInOrder: [],
      byId: {},
      byUser: {},
      reactionsByChirp: {},
    },
    [mockLaoId]: {
      allIdsInOrder: [chirp2.id, chirp1.id],
      byId: { [chirp1.id]: chirp1, [chirp2.id]: chirp2 },
      byUser: { [chirp1.sender]: [chirp1.id], [chirp2.sender]: [chirp2.id] },
      reactionsByChirp: {},
    },
  },
};

const chirpFilledState3 = {
  byLaoId: {
    myLaoId: {
      allIdsInOrder: [],
      byId: {},
      byUser: {},
      reactionsByChirp: {},
    },
    [mockLaoId]: {
      allIdsInOrder: [chirp2.id, chirp3.id, chirp1.id],
      byId: { [chirp1.id]: chirp1, [chirp2.id]: chirp2, [chirp3.id]: chirp3 },
      byUser: { [chirp1.sender]: [chirp3.id, chirp1.id], [chirp2.sender]: [chirp2.id] },
      reactionsByChirp: {},
    },
  },
};

const chirpFilledState4 = {
  byLaoId: {
    myLaoId: {
      allIdsInOrder: [],
      byId: {},
      byUser: {},
      reactionsByChirp: {},
    },
    [mockLaoId]: {
      allIdsInOrder: [chirp4.id, chirp2.id, chirp3.id, chirp1.id],
      byId: {
        [chirp1.id]: chirp1,
        [chirp2.id]: chirp2,
        [chirp3.id]: chirp3,
        [chirp4.id]: chirp4,
      },
      byUser: { [chirp1.sender]: [chirp4.id, chirp3.id, chirp1.id], [chirp2.sender]: [chirp2.id] },
      reactionsByChirp: {},
    },
  },
};

const chirpFilledState5 = {
  byLaoId: {
    myLaoId: {
      allIdsInOrder: [],
      byId: {},
      byUser: {},
      reactionsByChirp: {},
    },
    [mockLaoId]: {
      allIdsInOrder: [chirp4.id, chirp2.id, chirp3.id, chirp1.id],
      byId: {
        [chirp1.id]: chirp1Deleted,
        [chirp2.id]: chirp2,
        [chirp3.id]: chirp3,
        [chirp4.id]: chirp4,
      },
      byUser: { [chirp1.sender]: [chirp4.id, chirp3.id, chirp1.id], [chirp2.sender]: [chirp2.id] },
      reactionsByChirp: {},
    },
  },
};

const reactionFilledState1 = {
  byLaoId: {
    myLaoId: {
      allIdsInOrder: [],
      byId: {},
      byUser: {},
      reactionsByChirp: {},
    },
    [mockLaoId]: {
      allIdsInOrder: [],
      byId: {},
      byUser: {},
      reactionsByChirp: { [mockChirpId1.toString()]: { '👍': [mockSender1.toString()] } },
    },
  },
};

const reactionFilledState11 = {
  byLaoId: {
    myLaoId: {
      allIdsInOrder: [],
      byId: {},
      byUser: {},
      reactionsByChirp: {},
    },
    [mockLaoId]: {
      allIdsInOrder: [chirp1.id],
      byId: { [chirp1.id]: chirp1 },
      byUser: { [chirp1.sender]: [chirp1.id] },
      reactionsByChirp: { [mockChirpId1.toString()]: { '👍': [mockSender1.toString()] } },
    },
  },
};

const reactionFilledState2 = {
  byLaoId: {
    myLaoId: {
      allIdsInOrder: [],
      byId: {},
      byUser: {},
      reactionsByChirp: {},
    },
    [mockLaoId]: {
      allIdsInOrder: [],
      byId: {},
      byUser: {},
      reactionsByChirp: {
        [mockChirpId1.toString()]: {
          '👍': [mockSender1.toString()],
          '❤️': [mockSender1.toString()],
        },
      },
    },
  },
};

const reactionFilledState22 = {
  byLaoId: {
    myLaoId: {
      allIdsInOrder: [],
      byId: {},
      byUser: {},
      reactionsByChirp: {},
    },
    [mockLaoId]: {
      allIdsInOrder: [chirp1.id],
      byId: { [chirp1.id]: chirp1 },
      byUser: { [chirp1.sender]: [chirp1.id] },
      reactionsByChirp: {
        [mockChirpId1.toString()]: {
          '👍': [mockSender1.toString()],
          '❤️': [mockSender1.toString()],
        },
      },
    },
  },
};

const reactionFilledState3 = {
  byLaoId: {
    myLaoId: {
      allIdsInOrder: [],
      byId: {},
      byUser: {},
      reactionsByChirp: {},
    },
    [mockLaoId]: {
      allIdsInOrder: [],
      byId: {},
      byUser: {},
      reactionsByChirp: {
        [mockChirpId1.toString()]: {
          '👍': [mockSender1.toString(), mockSender2.toString()],
          '❤️': [mockSender1.toString()],
        },
      },
    },
  },
};

const reactionFilledState33 = {
  byLaoId: {
    myLaoId: {
      allIdsInOrder: [],
      byId: {},
      byUser: {},
      reactionsByChirp: {},
    },
    [mockLaoId]: {
      allIdsInOrder: [chirp1.id],
      byId: { [chirp1.id]: chirp1 },
      byUser: { [chirp1.sender]: [chirp1.id] },
      reactionsByChirp: {
        [mockChirpId1.toString()]: {
          '👍': [mockSender1.toString(), mockSender2.toString()],
          '❤️': [mockSender1.toString()],
        },
      },
    },
  },
};

const reactionFilledState4 = {
  byLaoId: {
    myLaoId: {
      allIdsInOrder: [],
      byId: {},
      byUser: {},
      reactionsByChirp: {},
    },
    [mockLaoId]: {
      allIdsInOrder: [],
      byId: {},
      byUser: {},
      reactionsByChirp: {
        [mockChirpId1.toString()]: { '👍': [mockSender1.toString()] },
        [mockChirpId2.toString()]: { '👍': [mockSender2.toString()] },
      },
    },
  },
};

const reactionFilledState44 = {
  byLaoId: {
    myLaoId: {
      allIdsInOrder: [],
      byId: {},
      byUser: {},
      reactionsByChirp: {},
    },
    [mockLaoId]: {
      allIdsInOrder: [chirp2.id, chirp1.id],
      byId: { [chirp1.id]: chirp1, [chirp2.id]: chirp2 },
      byUser: { [chirp1.sender]: [chirp1.id], [chirp2.sender]: [chirp2.id] },
      reactionsByChirp: {
        [mockChirpId1.toString()]: { '👍': [mockSender1.toString()] },
        [mockChirpId2.toString()]: { '👍': [mockSender2.toString()] },
      },
    },
  },
};

describe('social reducer', () => {
  describe('chirp reducer', () => {
    test('reducer should return the initial state', () => {
      expect(socialReduce(undefined, {} as AnyAction))
        .toEqual(emptyState);
    });

    describe('add chirp', () => {
      test('reducer should add the first chirp correctly', () => {
        expect(socialReduce(emptyState, addChirp(mockLaoId, chirp1)))
          .toEqual(chirpFilledState1);
      });

      test('reducer should add the newer chirp before the first chirp', () => {
        expect(socialReduce(chirpFilledState1, addChirp(mockLaoId, chirp2)))
          .toEqual(chirpFilledState2);
      });

      test('reducer should add the newer chirp after the second chirp', () => {
        expect(socialReduce(chirpFilledState2, addChirp(mockLaoId, chirp3)))
          .toEqual(chirpFilledState3);
      });

      test('reducer should add the newest chirp on top', () => {
        expect(socialReduce(chirpFilledState3, addChirp(mockLaoId, chirp4)))
          .toEqual(chirpFilledState4);
      });
    });

    describe('delete chirp', () => {
      test('reducer should mark chirp 1 as deleted', () => {
        expect(socialReduce(chirpFilledState4, deleteChirp(mockLaoId, chirp1Deleted)))
          .toEqual(chirpFilledState5);
      });

      test('delete a non-stored chirp should do nothing', () => {
        expect(socialReduce(chirpFilledState4, deleteChirp(mockLaoId, chirp0)))
          .toEqual(chirpFilledState4);
      });

      test('reducer should not re-add a chirp if it has already been deleted', () => {
        expect(socialReduce(chirpFilledState5, addChirp(mockLaoId, chirp1Deleted)))
          .toEqual(chirpFilledState5);
      });
    });
  });

  describe('reaction reducer', () => {
    test('reducer should create entry for a chirp when receiving the first reaction on it', () => {
      expect(socialReduce(emptyState, addReaction(mockLaoId, reaction1)))
        .toEqual(reactionFilledState1);
    });

    test('reducer should add reaction codepoint to an existing chirp', () => {
      expect(socialReduce(reactionFilledState1, addReaction(mockLaoId, reaction2)))
        .toEqual(reactionFilledState2);
    });

    test('reducer should add new reaction sender for a chirp', () => {
      expect(socialReduce(reactionFilledState2, addReaction(mockLaoId, reaction3)))
        .toEqual(reactionFilledState3);
    });

    test('reducer should not add existing sender of a reaction for a chirp', () => {
      expect(socialReduce(reactionFilledState3, addReaction(mockLaoId, reaction1)))
        .toEqual(reactionFilledState3);
    });

    test('reducer should create new chirp entry correctly', () => {
      expect(socialReduce(reactionFilledState1, addReaction(mockLaoId, reaction4)))
        .toEqual(reactionFilledState4);
    });
  });
});

describe('social selector', () => {
  describe('chirp selector', () => {
    test('selector should return an empty list of chirpState when no lao is opened', () => {
      expect(makeChirpsList().resultFunc(emptyState, undefined))
        .toEqual([]);
    });

    test('selector should return an empty list', () => {
      expect(makeChirpsList().resultFunc(emptyState, mockLaoId))
        .toEqual([]);
    });

    test('selector should return the first chirp state', () => {
      expect(makeChirpsList().resultFunc(chirpFilledState1, mockLaoId))
        .toEqual([chirp1]);
    });

    test('selector should return the newer chirp before the first chirp', () => {
      expect(makeChirpsList().resultFunc(chirpFilledState2, mockLaoId))
        .toEqual([chirp2, chirp1]);
    });

    test('selector should add the newer chirp after the second chirp', () => {
      expect(makeChirpsList().resultFunc(chirpFilledState3, mockLaoId))
        .toEqual([chirp2, chirp3, chirp1]);
    });

    test('selector should return the newest chirp on top', () => {
      expect(makeChirpsList().resultFunc(chirpFilledState4, mockLaoId))
        .toEqual([chirp4, chirp2, chirp3, chirp1]);
    });
  });

  describe('reaction selector', () => {
    test('selector should return an empty record of reactionState when no lao is opened', () => {
      expect(makeReactionsList().resultFunc(emptyState, undefined))
        .toEqual({});
    });

    test('selector should return an empty record', () => {
      expect(makeReactionsList().resultFunc(emptyState, mockLaoId))
        .toEqual({});
    });

    test('selector should return an empty record for non-stored chirp', () => {
      expect(makeReactionsList().resultFunc(reactionFilledState1, mockLaoId))
        .toEqual({});
    });

    test('selector should return the first reaction state', () => {
      expect(makeReactionsList().resultFunc(reactionFilledState11, mockLaoId))
        .toEqual({ [mockChirpId1.toString()]: { '👍': 1, '👎': 0, '❤️': 0 } });
    });

    test('selector should add reaction count correctly', () => {
      expect(makeReactionsList().resultFunc(reactionFilledState22, mockLaoId))
        .toEqual({ [mockChirpId1.toString()]: { '👍': 1, '👎': 0, '❤️': 1 } });
    });

    test('selector should increment counter for new sender', () => {
      expect(makeReactionsList().resultFunc(reactionFilledState33, mockLaoId))
        .toEqual({ [mockChirpId1.toString()]: { '👍': 2, '👎': 0, '❤️': 1 } });
    });

    test('selector should not count a sender twice for a reaction', () => {
      expect(makeReactionsList().resultFunc(reactionFilledState33, mockLaoId))
        .toEqual({ [mockChirpId1.toString()]: { '👍': 2, '👎': 0, '❤️': 1 } });
    });

    test('selector should return state of two reaction', () => {
      expect(makeReactionsList().resultFunc(reactionFilledState44, mockLaoId))
        .toEqual({
          [mockChirpId1.toString()]: { '👍': 1, '👎': 0, '❤️': 0 },
          [mockChirpId2.toString()]: { '👍': 1, '👎': 0, '❤️': 0 },
        });
    });
  });
});
