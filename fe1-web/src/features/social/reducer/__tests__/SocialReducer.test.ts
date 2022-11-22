import 'jest-extended';

import { describe } from '@jest/globals';
import { AnyAction } from 'redux';

import { serializedMockLaoId, mockLaoId } from '__tests__/utils/TestUtils';
import { Hash, PublicKey, Timestamp } from 'core/objects';

import { Chirp, ChirpState, Reaction, ReactionState } from '../../objects';
import {
  addChirp,
  addReaction,
  deleteChirp,
  makeChirpsList,
  makeChirpsListOfUser,
  makeReactionsList,
  socialReduce,
} from '../SocialReducer';

// region test data
let chirp0DeletedFake: ChirpState;
let chirp0: ChirpState;
let chirp1: ChirpState;
let chirp1Deleted: ChirpState;
let chirp1DeletedFake: ChirpState;
let chirp2: ChirpState;
let chirp3: ChirpState;
let chirp4: ChirpState;

let mockChirpId1: Hash;
let mockChirpId2: Hash;

let reaction1: ReactionState;
let reaction2: ReactionState;
let reaction3: ReactionState;
let reaction4: ReactionState;

let emptyState: any;
let chirpFilledState0Deleted: any;
let chirpFilledState0Added: any;
let chirpFilledState1: any;
let chirpFilledState2: any;
let chirpFilledState3: any;
let chirpFilledState4: any;
let chirpFilledState4Chirp1Deleted: any;
let chirpFilledState4Chirp4Deleted: any;

let reactionFilledState1: any;
let reactionFilledState11: any;
let reactionFilledState2: any;
let reactionFilledState22: any;
let reactionFilledState3: any;
let reactionFilledState33: any;
let reactionFilledState4: any;
let reactionFilledState44: any;

const initialiseData = () => {
  const mockSender1: PublicKey = new PublicKey('Douglas Adams');
  const mockSender2: PublicKey = new PublicKey('Gandalf');
  const mockChirpId0: Hash = new Hash('000');
  mockChirpId1 = new Hash('1234');
  mockChirpId2 = new Hash('5678');
  const mockChirpId3: Hash = new Hash('123456');
  const mockTimestamp: Timestamp = new Timestamp(1606666600);

  chirp0DeletedFake = new Chirp({
    id: mockChirpId0,
    sender: new PublicKey('Joker'),
    text: '',
    time: mockTimestamp,
    isDeleted: true,
  }).toState();

  chirp0 = new Chirp({
    id: mockChirpId0,
    sender: mockSender1,
    text: "Don't delete me!",
    time: mockTimestamp,
    isDeleted: false,
  }).toState();

  chirp1 = new Chirp({
    id: mockChirpId1,
    sender: mockSender1,
    text: "Don't panic.",
    time: new Timestamp(1605555500),
    isDeleted: false,
  }).toState();

  chirp1Deleted = new Chirp({
    id: mockChirpId1,
    sender: mockSender1,
    text: '',
    time: new Timestamp(1605555500),
    isDeleted: true,
  }).toState();

  chirp1DeletedFake = new Chirp({
    id: mockChirpId1,
    sender: mockSender2,
    text: '',
    time: new Timestamp(1605555500),
    isDeleted: true,
  }).toState();

  chirp2 = new Chirp({
    id: mockChirpId2,
    sender: mockSender2,
    text: 'You shall not pass! You shall not pass! You shall not pass! You shall not pass! You shall not pass! You shall not pass!',
    time: new Timestamp(1607777700),
  }).toState();

  chirp3 = new Chirp({
    id: new Hash('12345'),
    sender: mockSender1,
    text: 'Time is an illusion',
    time: mockTimestamp,
  }).toState();

  chirp4 = new Chirp({
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

  reaction1 = new Reaction({
    id: new Hash('1111'),
    sender: mockSender1,
    codepoint: '👍',
    chirpId: mockChirpId1,
    time: mockTimestamp,
  }).toState();

  reaction2 = new Reaction({
    id: new Hash('2222'),
    sender: mockSender1,
    codepoint: '❤️',
    chirpId: mockChirpId1,
    time: mockTimestamp,
  }).toState();

  reaction3 = new Reaction({
    id: new Hash('3333'),
    sender: mockSender2,
    codepoint: '👍',
    chirpId: mockChirpId1,
    time: mockTimestamp,
  }).toState();

  reaction4 = new Reaction({
    id: new Hash('4444'),
    sender: mockSender2,
    codepoint: '👍',
    chirpId: mockChirpId2,
    time: mockTimestamp,
  }).toState();

  emptyState = {
    byLaoId: {
      myLaoId: {
        allIdsInOrder: [],
        byId: {},
        byUser: {},
        reactionsByChirp: {},
      },
    },
  };

  chirpFilledState0Deleted = {
    byLaoId: {
      myLaoId: {
        allIdsInOrder: [],
        byId: {},
        byUser: {},
        reactionsByChirp: {},
      },
      [serializedMockLaoId]: {
        allIdsInOrder: [],
        byId: { [mockChirpId0.toString()]: chirp0DeletedFake },
        byUser: {},
        reactionsByChirp: {},
      },
    },
  };

  chirpFilledState0Added = {
    byLaoId: {
      myLaoId: {
        allIdsInOrder: [],
        byId: {},
        byUser: {},
        reactionsByChirp: {},
      },
      [serializedMockLaoId]: {
        allIdsInOrder: [chirp0.id],
        byId: { [chirp0.id]: chirp0 },
        byUser: { [chirp0.sender]: [chirp0.id] },
        reactionsByChirp: {},
      },
    },
  };

  chirpFilledState1 = {
    byLaoId: {
      myLaoId: {
        allIdsInOrder: [],
        byId: {},
        byUser: {},
        reactionsByChirp: {},
      },
      [serializedMockLaoId]: {
        allIdsInOrder: [chirp1.id],
        byId: { [chirp1.id]: chirp1 },
        byUser: { [chirp1.sender]: [chirp1.id] },
        reactionsByChirp: {},
      },
    },
  };

  chirpFilledState2 = {
    byLaoId: {
      myLaoId: {
        allIdsInOrder: [],
        byId: {},
        byUser: {},
        reactionsByChirp: {},
      },
      [serializedMockLaoId]: {
        allIdsInOrder: [chirp2.id, chirp1.id],
        byId: {
          [chirp1.id]: chirp1,
          [chirp2.id]: chirp2,
        },
        byUser: {
          [chirp1.sender]: [chirp1.id],
          [chirp2.sender]: [chirp2.id],
        },
        reactionsByChirp: {},
      },
    },
  };

  chirpFilledState3 = {
    byLaoId: {
      myLaoId: {
        allIdsInOrder: [],
        byId: {},
        byUser: {},
        reactionsByChirp: {},
      },
      [serializedMockLaoId]: {
        allIdsInOrder: [chirp2.id, chirp3.id, chirp1.id],
        byId: {
          [chirp1.id]: chirp1,
          [chirp2.id]: chirp2,
          [chirp3.id]: chirp3,
        },
        byUser: {
          [chirp1.sender]: [chirp3.id, chirp1.id],
          [chirp2.sender]: [chirp2.id],
        },
        reactionsByChirp: {},
      },
    },
  };

  chirpFilledState4 = {
    byLaoId: {
      myLaoId: {
        allIdsInOrder: [],
        byId: {},
        byUser: {},
        reactionsByChirp: {},
      },
      [serializedMockLaoId]: {
        allIdsInOrder: [chirp4.id, chirp2.id, chirp3.id, chirp1.id],
        byId: {
          [chirp1.id]: chirp1,
          [chirp2.id]: chirp2,
          [chirp3.id]: chirp3,
          [chirp4.id]: chirp4,
        },
        byUser: {
          [chirp1.sender]: [chirp4.id, chirp3.id, chirp1.id],
          [chirp2.sender]: [chirp2.id],
        },
        reactionsByChirp: {},
      },
    },
  };

  chirpFilledState4Chirp1Deleted = {
    byLaoId: {
      myLaoId: {
        allIdsInOrder: [],
        byId: {},
        byUser: {},
        reactionsByChirp: {},
      },
      [serializedMockLaoId]: {
        allIdsInOrder: [chirp4.id, chirp2.id, chirp3.id, chirp1.id],
        byId: {
          [chirp1.id]: chirp1Deleted,
          [chirp2.id]: chirp2,
          [chirp3.id]: chirp3,
          [chirp4.id]: chirp4,
        },
        byUser: {
          [chirp1.sender]: [chirp4.id, chirp3.id, chirp1.id],
          [chirp2.sender]: [chirp2.id],
        },
        reactionsByChirp: {},
      },
    },
  };

  chirpFilledState4Chirp4Deleted = {
    byLaoId: {
      myLaoId: {
        allIdsInOrder: [],
        byId: {},
        byUser: {},
        reactionsByChirp: {},
      },
      [serializedMockLaoId]: {
        allIdsInOrder: [chirp4.id, chirp2.id, chirp3.id, chirp1.id],
        byId: {
          [chirp1.id]: chirp1,
          [chirp2.id]: chirp2,
          [chirp3.id]: chirp3,
          [chirp4.id]: chirp4Deleted,
        },
        byUser: {
          [chirp1.sender]: [chirp4.id, chirp3.id, chirp1.id],
          [chirp2.sender]: [chirp2.id],
        },
        reactionsByChirp: {},
      },
    },
  };

  reactionFilledState1 = {
    byLaoId: {
      myLaoId: {
        allIdsInOrder: [],
        byId: {},
        byUser: {},
        reactionsByChirp: {},
      },
      [serializedMockLaoId]: {
        allIdsInOrder: [],
        byId: {},
        byUser: {},
        reactionsByChirp: { [mockChirpId1.toString()]: { '👍': [mockSender1.toString()] } },
      },
    },
  };

  reactionFilledState11 = {
    byLaoId: {
      myLaoId: {
        allIdsInOrder: [],
        byId: {},
        byUser: {},
        reactionsByChirp: {},
      },
      [serializedMockLaoId]: {
        allIdsInOrder: [chirp1.id],
        byId: { [chirp1.id]: chirp1 },
        byUser: { [chirp1.sender]: [chirp1.id] },
        reactionsByChirp: { [mockChirpId1.toString()]: { '👍': [mockSender1.toString()] } },
      },
    },
  };

  reactionFilledState2 = {
    byLaoId: {
      myLaoId: {
        allIdsInOrder: [],
        byId: {},
        byUser: {},
        reactionsByChirp: {},
      },
      [serializedMockLaoId]: {
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

  reactionFilledState22 = {
    byLaoId: {
      myLaoId: {
        allIdsInOrder: [],
        byId: {},
        byUser: {},
        reactionsByChirp: {},
      },
      [serializedMockLaoId]: {
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

  reactionFilledState3 = {
    byLaoId: {
      myLaoId: {
        allIdsInOrder: [],
        byId: {},
        byUser: {},
        reactionsByChirp: {},
      },
      [serializedMockLaoId]: {
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

  reactionFilledState33 = {
    byLaoId: {
      myLaoId: {
        allIdsInOrder: [],
        byId: {},
        byUser: {},
        reactionsByChirp: {},
      },
      [serializedMockLaoId]: {
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

  reactionFilledState4 = {
    byLaoId: {
      myLaoId: {
        allIdsInOrder: [],
        byId: {},
        byUser: {},
        reactionsByChirp: {},
      },
      [serializedMockLaoId]: {
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

  reactionFilledState44 = {
    byLaoId: {
      myLaoId: {
        allIdsInOrder: [],
        byId: {},
        byUser: {},
        reactionsByChirp: {},
      },
      [serializedMockLaoId]: {
        allIdsInOrder: [chirp2.id, chirp1.id],
        byId: {
          [chirp1.id]: chirp1,
          [chirp2.id]: chirp2,
        },
        byUser: {
          [chirp1.sender]: [chirp1.id],
          [chirp2.sender]: [chirp2.id],
        },
        reactionsByChirp: {
          [mockChirpId1.toString()]: { '👍': [mockSender1.toString()] },
          [mockChirpId2.toString()]: { '👍': [mockSender2.toString()] },
        },
      },
    },
  };
};
// endregion

beforeEach(() => {
  initialiseData();
});

describe('SocialReducer', () => {
  describe('chirp reducer', () => {
    it('should return the initial state', () => {
      expect(socialReduce(undefined, {} as AnyAction)).toEqual(emptyState);
    });

    it('should add the first chirp correctly', () => {
      expect(socialReduce(emptyState, addChirp(mockLaoId, chirp1))).toEqual(chirpFilledState1);
    });

    it('should add the newer chirp before the first chirp', () => {
      expect(socialReduce(chirpFilledState1, addChirp(mockLaoId, chirp2))).toEqual(
        chirpFilledState2,
      );
    });

    it('should add the newer chirp after the second chirp', () => {
      expect(socialReduce(chirpFilledState2, addChirp(mockLaoId, chirp3))).toEqual(
        chirpFilledState3,
      );
    });

    it('should add the newest chirp on top', () => {
      expect(socialReduce(chirpFilledState3, addChirp(mockLaoId, chirp4))).toEqual(
        chirpFilledState4,
      );
    });

    it('should mark chirp 1 as deleted', () => {
      expect(socialReduce(chirpFilledState4, deleteChirp(mockLaoId, chirp1Deleted))).toEqual(
        chirpFilledState4Chirp1Deleted,
      );
    });

    it('delete a non-stored chirp should store it in byId as deleted', () => {
      expect(socialReduce(emptyState, deleteChirp(mockLaoId, chirp0DeletedFake))).toEqual(
        chirpFilledState0Deleted,
      );
    });

    it('should ignore delete request sent by non-original sender', () => {
      expect(socialReduce(chirpFilledState4, deleteChirp(mockLaoId, chirp1DeletedFake))).toEqual(
        chirpFilledState4,
      );
    });

    it('should update/add a chirp if it has been deleted by a different sender', () => {
      expect(socialReduce(chirpFilledState0Deleted, addChirp(mockLaoId, chirp0))).toEqual(
        chirpFilledState0Added,
      );
    });

    it('should not re-add a chirp if it has already been deleted by the same sender', () => {
      const stateDeleted = socialReduce(chirpFilledState3, deleteChirp(mockLaoId, chirp4));
      expect(socialReduce(stateDeleted, addChirp(mockLaoId, chirp4))).toEqual(
        chirpFilledState4Chirp4Deleted,
      );
    });
  });

  describe('chirp selector', () => {
    it('should return an empty list of chirpState when no lao is opened', () => {
      expect(makeChirpsList(mockLaoId).resultFunc(emptyState)).toEqual([]);
    });

    it('should return an empty list', () => {
      expect(makeChirpsList(mockLaoId).resultFunc(emptyState)).toEqual([]);
    });

    it('should return the first chirp state', () => {
      expect(makeChirpsList(mockLaoId).resultFunc(chirpFilledState1)).toEqual([chirp1]);
    });

    it('should return the newer chirp before the first chirp', () => {
      expect(makeChirpsList(mockLaoId).resultFunc(chirpFilledState2)).toEqual([chirp2, chirp1]);
    });

    it('should add the newer chirp after the second chirp', () => {
      expect(makeChirpsList(mockLaoId).resultFunc(chirpFilledState3)).toEqual([
        chirp2,
        chirp3,
        chirp1,
      ]);
    });

    it('should return the newest chirp on top', () => {
      expect(makeChirpsList(mockLaoId).resultFunc(chirpFilledState4)).toEqual([
        chirp4,
        chirp2,
        chirp3,
        chirp1,
      ]);
    });

    it('should return the correct chirps list for an active user', () => {
      expect(
        makeChirpsListOfUser(mockLaoId)(new PublicKey(chirp1.sender)).resultFunc(chirpFilledState3),
      ).toEqual([chirp3, chirp1]);
    });

    it('should return an empty list for an inactive user', () => {
      expect(
        makeChirpsListOfUser(mockLaoId)(new PublicKey(chirp2.sender)).resultFunc(chirpFilledState1),
      ).toEqual([]);
    });

    it('should return an empty list for an undefined lao', () => {
      expect(
        makeChirpsListOfUser(mockLaoId)(new PublicKey(chirp2.sender)).resultFunc(chirpFilledState1),
      ).toEqual([]);
    });
  });

  describe('reaction reducer', () => {
    it('should create entry for a chirp when receiving the first reaction on it', () => {
      expect(socialReduce(emptyState, addReaction(mockLaoId, reaction1))).toEqual(
        reactionFilledState1,
      );
    });

    it('should add reaction codepoint to an existing chirp', () => {
      expect(socialReduce(reactionFilledState1, addReaction(mockLaoId, reaction2))).toEqual(
        reactionFilledState2,
      );
    });

    it('should add new reaction sender for a chirp', () => {
      expect(socialReduce(reactionFilledState2, addReaction(mockLaoId, reaction3))).toEqual(
        reactionFilledState3,
      );
    });

    it('should not add existing sender of a reaction for a chirp', () => {
      expect(socialReduce(reactionFilledState3, addReaction(mockLaoId, reaction1))).toEqual(
        reactionFilledState3,
      );
    });

    it('should create new chirp entry correctly', () => {
      expect(socialReduce(reactionFilledState1, addReaction(mockLaoId, reaction4))).toEqual(
        reactionFilledState4,
      );
    });
  });

  describe('reaction selector', () => {
    it('should return an empty record of reactionState when no lao is opened', () => {
      expect(makeReactionsList(mockLaoId).resultFunc(emptyState)).toEqual({});
    });

    it('should return an empty record', () => {
      expect(makeReactionsList(mockLaoId).resultFunc(emptyState)).toEqual({});
    });

    it('should return an empty record for non-stored chirp', () => {
      expect(makeReactionsList(mockLaoId).resultFunc(reactionFilledState1)).toEqual({});
    });

    it('should return the first reaction state', () => {
      expect(makeReactionsList(mockLaoId).resultFunc(reactionFilledState11)).toEqual({
        [mockChirpId1.toString()]: {
          '👍': 1,
          '👎': 0,
          '❤️': 0,
        },
      });
    });

    it('should add reaction count correctly', () => {
      expect(makeReactionsList(mockLaoId).resultFunc(reactionFilledState22)).toEqual({
        [mockChirpId1.toString()]: {
          '👍': 1,
          '👎': 0,
          '❤️': 1,
        },
      });
    });

    it('should increment counter for new sender', () => {
      expect(makeReactionsList(mockLaoId).resultFunc(reactionFilledState33)).toEqual({
        [mockChirpId1.toString()]: {
          '👍': 2,
          '👎': 0,
          '❤️': 1,
        },
      });
    });

    it('should not count a sender twice for a reaction', () => {
      expect(makeReactionsList(mockLaoId).resultFunc(reactionFilledState33)).toEqual({
        [mockChirpId1.toString()]: {
          '👍': 2,
          '👎': 0,
          '❤️': 1,
        },
      });
    });

    it('should return state of two reaction', () => {
      expect(makeReactionsList(mockLaoId).resultFunc(reactionFilledState44)).toEqual({
        [mockChirpId1.toString()]: {
          '👍': 1,
          '👎': 0,
          '❤️': 0,
        },
        [mockChirpId2.toString()]: {
          '👍': 1,
          '👎': 0,
          '❤️': 0,
        },
      });
    });
  });
});
