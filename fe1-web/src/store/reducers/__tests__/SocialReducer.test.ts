import 'jest-extended';
import { AnyAction } from 'redux';
import {
  Chirp, Hash, PublicKey, Timestamp, Reaction,
} from 'model/objects';
import { describe } from '@jest/globals';
import { mockLaoId } from '__tests__/utils/TestUtils';
import {
  socialReduce,
  addChirp,
  makeChirpsList,
  makeChirpsListOfUser,
  deleteChirp,
  addReaction,
  makeReactionsList,
} from '../SocialReducer';

const mockSender1: PublicKey = new PublicKey('Douglas Adams');
const mockSender2: PublicKey = new PublicKey('Gandalf');
const mockChirpId0: Hash = Hash.fromString('000');
const mockChirpId1: Hash = Hash.fromString('1234');
const mockChirpId2: Hash = Hash.fromString('5678');
const mockChirpId3: Hash = Hash.fromString('123456');
const mockTimestamp: Timestamp = new Timestamp(1606666600);

const chirp0DeletedFake = new Chirp({
  id: mockChirpId0,
  sender: new PublicKey('Joker'),
  text: '',
  time: mockTimestamp,
  isDeleted: true,
}).toState();

const chirp0 = new Chirp({
  id: mockChirpId0,
  sender: mockSender1,
  text: 'Don\'t delete me!',
  time: mockTimestamp,
  isDeleted: false,
}).toState();

const chirp1 = new Chirp({
  id: mockChirpId1,
  sender: mockSender1,
  text: 'Don\'t panic.',
  time: new Timestamp(1605555500),
  isDeleted: false,
}).toState();

const chirp1Deleted = new Chirp({
  id: mockChirpId1,
  sender: mockSender1,
  text: '',
  time: new Timestamp(1605555500),
  isDeleted: true,
}).toState();

const chirp1DeletedFake = new Chirp({
  id: mockChirpId1,
  sender: mockSender2,
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
  id: mockChirpId3,
  sender: mockSender1,
  text: 'The answer is 42',
  time: new Timestamp(1608888800),
}).toState();

const chirp4Deleted = new Chirp({
  id: mockChirpId3,
  sender: mockSender1,
  text: '',
  time: new Timestamp(1608888800),
  isDeleted: true,
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

const chirpFilledState0Deleted = {
  byLaoId: {
    myLaoId: {
      allIdsInOrder: [],
      byId: {},
      byUser: {},
      reactionsByChirp: {},
    },
    [mockLaoId]: {
      allIdsInOrder: [],
      byId: { [mockChirpId0.toString()]: chirp0DeletedFake },
      byUser: {},
      reactionsByChirp: {},
    },
  },
};

const chirpFilledState0Added = {
  byLaoId: {
    myLaoId: {
      allIdsInOrder: [],
      byId: {},
      byUser: {},
      reactionsByChirp: {},
    },
    [mockLaoId]: {
      allIdsInOrder: [chirp0.id],
      byId: { [chirp0.id]: chirp0 },
      byUser: { [chirp0.sender]: [chirp0.id] },
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

const chirpFilledState4Chirp1Deleted = {
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

const chirpFilledState4Chirp4Deleted = {
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
        [chirp4.id]: chirp4Deleted,
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

describe('SocialReducer', () => {
  describe('chirp reducer', () => {
    it('should return the initial state', () => {
      expect(socialReduce(undefined, {} as AnyAction))
        .toEqual(emptyState);
    });

    it('should add the first chirp correctly', () => {
      expect(socialReduce(emptyState, addChirp(mockLaoId, chirp1)))
        .toEqual(chirpFilledState1);
    });

    it('should add the newer chirp before the first chirp', () => {
      expect(socialReduce(chirpFilledState1, addChirp(mockLaoId, chirp2)))
        .toEqual(chirpFilledState2);
    });

    it('should add the newer chirp after the second chirp', () => {
      expect(socialReduce(chirpFilledState2, addChirp(mockLaoId, chirp3)))
        .toEqual(chirpFilledState3);
    });

    it('should add the newest chirp on top', () => {
      expect(socialReduce(chirpFilledState3, addChirp(mockLaoId, chirp4)))
        .toEqual(chirpFilledState4);
    });

    it('should mark chirp 1 as deleted', () => {
      expect(socialReduce(chirpFilledState4, deleteChirp(mockLaoId, chirp1Deleted)))
        .toEqual(chirpFilledState4Chirp1Deleted);
    });

    it('delete a non-stored chirp should store it in byId as deleted', () => {
      expect(socialReduce(emptyState, deleteChirp(mockLaoId, chirp0DeletedFake)))
        .toEqual(chirpFilledState0Deleted);
    });

    it('should ignore delete request sent by non-original sender', () => {
      expect(socialReduce(chirpFilledState4, deleteChirp(mockLaoId, chirp1DeletedFake)))
        .toEqual(chirpFilledState4);
    });

    it('should update/add a chirp if it has been deleted by a different sender', () => {
      expect(socialReduce(chirpFilledState0Deleted, addChirp(mockLaoId, chirp0)))
        .toEqual(chirpFilledState0Added);
    });

    it('should not re-add a chirp if it has already been deleted by the same sender', () => {
      const stateDeleted = socialReduce(chirpFilledState3, deleteChirp(mockLaoId, chirp4));
      expect(socialReduce(stateDeleted, addChirp(mockLaoId, chirp4)))
        .toEqual(chirpFilledState4Chirp4Deleted);
    });
  });

  describe('chirp selector', () => {
    it('should return an empty list of chirpState when no lao is opened', () => {
      expect(makeChirpsList().resultFunc(emptyState, undefined))
        .toEqual([]);
    });

    it('should return an empty list', () => {
      expect(makeChirpsList().resultFunc(emptyState, mockLaoId))
        .toEqual([]);
    });

    it('should return the first chirp state', () => {
      expect(makeChirpsList().resultFunc(chirpFilledState1, mockLaoId))
        .toEqual([chirp1]);
    });

    it('should return the newer chirp before the first chirp', () => {
      expect(makeChirpsList().resultFunc(chirpFilledState2, mockLaoId))
        .toEqual([chirp2, chirp1]);
    });

    it('should add the newer chirp after the second chirp', () => {
      expect(makeChirpsList().resultFunc(chirpFilledState3, mockLaoId))
        .toEqual([chirp2, chirp3, chirp1]);
    });

    it('should return the newest chirp on top', () => {
      expect(makeChirpsList().resultFunc(chirpFilledState4, mockLaoId))
        .toEqual([chirp4, chirp2, chirp3, chirp1]);
    });

    it('should return the correct chirps list for an active user', () => {
      expect(makeChirpsListOfUser(chirp1.sender).resultFunc(chirpFilledState3, mockLaoId))
        .toEqual([chirp3, chirp1]);
    });

    it('should return an empty list for an inactive user', () => {
      expect(makeChirpsListOfUser(chirp2.sender).resultFunc(chirpFilledState1, mockLaoId))
        .toEqual([]);
    });

    it('should return an empty list for an undefined lao', () => {
      expect(makeChirpsListOfUser(chirp2.sender).resultFunc(chirpFilledState1, undefined))
        .toEqual([]);
    });
  });

  describe('reaction reducer', () => {
    it('should create entry for a chirp when receiving the first reaction on it', () => {
      expect(socialReduce(emptyState, addReaction(mockLaoId, reaction1)))
        .toEqual(reactionFilledState1);
    });

    it('should add reaction codepoint to an existing chirp', () => {
      expect(socialReduce(reactionFilledState1, addReaction(mockLaoId, reaction2)))
        .toEqual(reactionFilledState2);
    });

    it('should add new reaction sender for a chirp', () => {
      expect(socialReduce(reactionFilledState2, addReaction(mockLaoId, reaction3)))
        .toEqual(reactionFilledState3);
    });

    it('should not add existing sender of a reaction for a chirp', () => {
      expect(socialReduce(reactionFilledState3, addReaction(mockLaoId, reaction1)))
        .toEqual(reactionFilledState3);
    });

    it('should create new chirp entry correctly', () => {
      expect(socialReduce(reactionFilledState1, addReaction(mockLaoId, reaction4)))
        .toEqual(reactionFilledState4);
    });
  });

  describe('reaction selector', () => {
    it('should return an empty record of reactionState when no lao is opened', () => {
      expect(makeReactionsList().resultFunc(emptyState, undefined))
        .toEqual({});
    });

    it('should return an empty record', () => {
      expect(makeReactionsList().resultFunc(emptyState, mockLaoId))
        .toEqual({});
    });

    it('should return an empty record for non-stored chirp', () => {
      expect(makeReactionsList().resultFunc(reactionFilledState1, mockLaoId))
        .toEqual({});
    });

    it('should return the first reaction state', () => {
      expect(makeReactionsList().resultFunc(reactionFilledState11, mockLaoId))
        .toEqual({ [mockChirpId1.toString()]: { '👍': 1, '👎': 0, '❤️': 0 } });
    });

    it('should add reaction count correctly', () => {
      expect(makeReactionsList().resultFunc(reactionFilledState22, mockLaoId))
        .toEqual({ [mockChirpId1.toString()]: { '👍': 1, '👎': 0, '❤️': 1 } });
    });

    it('should increment counter for new sender', () => {
      expect(makeReactionsList().resultFunc(reactionFilledState33, mockLaoId))
        .toEqual({ [mockChirpId1.toString()]: { '👍': 2, '👎': 0, '❤️': 1 } });
    });

    it('should not count a sender twice for a reaction', () => {
      expect(makeReactionsList().resultFunc(reactionFilledState33, mockLaoId))
        .toEqual({ [mockChirpId1.toString()]: { '👍': 2, '👎': 0, '❤️': 1 } });
    });

    it('should return state of two reaction', () => {
      expect(makeReactionsList().resultFunc(reactionFilledState44, mockLaoId))
        .toEqual({
          [mockChirpId1.toString()]: { '👍': 1, '👎': 0, '❤️': 0 },
          [mockChirpId2.toString()]: { '👍': 1, '👎': 0, '❤️': 0 },
        });
    });
  });
});
