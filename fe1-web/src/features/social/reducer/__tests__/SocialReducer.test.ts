import 'jest-extended';

import { describe } from '@jest/globals';
import { AnyAction } from 'redux';

import { serializedMockLaoId, mockLaoId } from '__tests__/utils/TestUtils';
import { Hash, PublicKey, Timestamp } from 'core/objects';

import { Chirp, Reaction } from '../../objects';
import {
  addChirp,
  addReaction,
  deleteChirp,
  makeChirpsList,
  makeChirpsListOfUser,
  makeReactionsList,
  SocialLaoReducerState,
  socialReduce,
} from '../SocialReducer';

// region test data

const mockSender1: PublicKey = new PublicKey('Douglas Adams');
const mockSender2: PublicKey = new PublicKey('Gandalf');
const mockChirpId0: Hash = new Hash('000');
const mockChirpId1 = new Hash('1234');
const mockChirpId2 = new Hash('5678');
const mockChirpId3: Hash = new Hash('123456');
const mockTimestamp: Timestamp = new Timestamp(1606666600);

const chirp0DeletedFake = new Chirp({
  id: mockChirpId0,
  sender: new PublicKey('Joker'),
  text: '',
  time: mockTimestamp,
  isDeleted: true,
});

const chirp0 = new Chirp({
  id: mockChirpId0,
  sender: mockSender1,
  text: "Don't delete me!",
  time: mockTimestamp,
  isDeleted: false,
});

const chirp1 = new Chirp({
  id: mockChirpId1,
  sender: mockSender1,
  text: "Don't panic.",
  time: new Timestamp(1605555500),
  isDeleted: false,
});

const chirp1Deleted = new Chirp({
  id: mockChirpId1,
  sender: mockSender1,
  text: '',
  time: new Timestamp(1605555500),
  isDeleted: true,
});

const chirp1DeletedFake = new Chirp({
  id: mockChirpId1,
  sender: mockSender2,
  text: '',
  time: new Timestamp(1605555500),
  isDeleted: true,
});

const chirp2 = new Chirp({
  id: mockChirpId2,
  sender: mockSender2,
  text: 'You shall not pass! You shall not pass! You shall not pass! You shall not pass! You shall not pass! You shall not pass!',
  time: new Timestamp(1607777700),
});

const chirp3 = new Chirp({
  id: new Hash('12345'),
  sender: mockSender1,
  text: 'Time is an illusion',
  time: mockTimestamp,
});

const chirp4 = new Chirp({
  id: mockChirpId3,
  sender: mockSender1,
  text: 'The answer is 42',
  time: new Timestamp(1608888800),
});

const chirp4Deleted = new Chirp({
  id: mockChirpId3,
  sender: mockSender1,
  text: '',
  time: new Timestamp(1608888800),
  isDeleted: true,
});

const reaction1 = new Reaction({
  id: new Hash('1111'),
  sender: mockSender1,
  codepoint: '👍',
  chirpId: mockChirpId1,
  time: mockTimestamp,
});

const reaction2 = new Reaction({
  id: new Hash('2222'),
  sender: mockSender1,
  codepoint: '❤️',
  chirpId: mockChirpId1,
  time: mockTimestamp,
});

const reaction3 = new Reaction({
  id: new Hash('3333'),
  sender: mockSender2,
  codepoint: '👍',
  chirpId: mockChirpId1,
  time: mockTimestamp,
});

const reaction4 = new Reaction({
  id: new Hash('4444'),
  sender: mockSender2,
  codepoint: '👍',
  chirpId: mockChirpId2,
  time: mockTimestamp,
});

const emptyState: SocialLaoReducerState = {
  byLaoId: {
    myLaoId: {
      allIdsInOrder: [],
      byId: {},
      byUser: {},
      reactionsByChirp: {},
    },
  },
};

const chirpFilledState0Deleted: SocialLaoReducerState = {
  byLaoId: {
    myLaoId: {
      allIdsInOrder: [],
      byId: {},
      byUser: {},
      reactionsByChirp: {},
    },
    [serializedMockLaoId]: {
      allIdsInOrder: [],
      byId: { [mockChirpId0.toState()]: chirp0DeletedFake.toState() },
      byUser: {},
      reactionsByChirp: {},
    },
  },
};

const chirpFilledState0Added: SocialLaoReducerState = {
  byLaoId: {
    myLaoId: {
      allIdsInOrder: [],
      byId: {},
      byUser: {},
      reactionsByChirp: {},
    },
    [serializedMockLaoId]: {
      allIdsInOrder: [chirp0.id.toState()],
      byId: { [chirp0.id.toState()]: chirp0.toState() },
      byUser: { [chirp0.sender.toState()]: [chirp0.id.toState()] },
      reactionsByChirp: {},
    },
  },
};

const chirpFilledState1: SocialLaoReducerState = {
  byLaoId: {
    myLaoId: {
      allIdsInOrder: [],
      byId: {},
      byUser: {},
      reactionsByChirp: {},
    },
    [serializedMockLaoId]: {
      allIdsInOrder: [chirp1.id.toState()],
      byId: { [chirp1.id.toState()]: chirp1.toState() },
      byUser: { [chirp1.sender.toState()]: [chirp1.id.toState()] },
      reactionsByChirp: {},
    },
  },
};

const chirpFilledState2: SocialLaoReducerState = {
  byLaoId: {
    myLaoId: {
      allIdsInOrder: [],
      byId: {},
      byUser: {},
      reactionsByChirp: {},
    },
    [serializedMockLaoId]: {
      allIdsInOrder: [chirp2.id.toState(), chirp1.id.toState()],
      byId: {
        [chirp1.id.toState()]: chirp1.toState(),
        [chirp2.id.toState()]: chirp2.toState(),
      },
      byUser: {
        [chirp1.sender.toState()]: [chirp1.id.toState()],
        [chirp2.sender.toState()]: [chirp2.id.toState()],
      },
      reactionsByChirp: {},
    },
  },
};

const chirpFilledState3: SocialLaoReducerState = {
  byLaoId: {
    myLaoId: {
      allIdsInOrder: [],
      byId: {},
      byUser: {},
      reactionsByChirp: {},
    },
    [serializedMockLaoId]: {
      allIdsInOrder: [chirp2.id.toState(), chirp3.id.toState(), chirp1.id.toState()],
      byId: {
        [chirp1.id.toState()]: chirp1.toState(),
        [chirp2.id.toState()]: chirp2.toState(),
        [chirp3.id.toState()]: chirp3.toState(),
      },
      byUser: {
        [chirp1.sender.toState()]: [chirp3.id.toState(), chirp1.id.toState()],
        [chirp2.sender.toState()]: [chirp2.id.toState()],
      },
      reactionsByChirp: {},
    },
  },
};

const chirpFilledState4: SocialLaoReducerState = {
  byLaoId: {
    myLaoId: {
      allIdsInOrder: [],
      byId: {},
      byUser: {},
      reactionsByChirp: {},
    },
    [serializedMockLaoId]: {
      allIdsInOrder: [
        chirp4.id.toState(),
        chirp2.id.toState(),
        chirp3.id.toState(),
        chirp1.id.toState(),
      ],
      byId: {
        [chirp1.id.toState()]: chirp1.toState(),
        [chirp2.id.toState()]: chirp2.toState(),
        [chirp3.id.toState()]: chirp3.toState(),
        [chirp4.id.toState()]: chirp4.toState(),
      },
      byUser: {
        [chirp1.sender.toState()]: [chirp4.id.toState(), chirp3.id.toState(), chirp1.id.toState()],
        [chirp2.sender.toState()]: [chirp2.id.toState()],
      },
      reactionsByChirp: {},
    },
  },
};

const chirpFilledState4Chirp1Deleted: SocialLaoReducerState = {
  byLaoId: {
    myLaoId: {
      allIdsInOrder: [],
      byId: {},
      byUser: {},
      reactionsByChirp: {},
    },
    [serializedMockLaoId]: {
      allIdsInOrder: [
        chirp4.id.toState(),
        chirp2.id.toState(),
        chirp3.id.toState(),
        chirp1.id.toState(),
      ],
      byId: {
        [chirp1.id.toState()]: chirp1Deleted.toState(),
        [chirp2.id.toState()]: chirp2.toState(),
        [chirp3.id.toState()]: chirp3.toState(),
        [chirp4.id.toState()]: chirp4.toState(),
      },
      byUser: {
        [chirp1.sender.toState()]: [chirp4.id.toState(), chirp3.id.toState(), chirp1.id.toState()],
        [chirp2.sender.toState()]: [chirp2.id.toState()],
      },
      reactionsByChirp: {},
    },
  },
};

const chirpFilledState4Chirp4Deleted: SocialLaoReducerState = {
  byLaoId: {
    myLaoId: {
      allIdsInOrder: [],
      byId: {},
      byUser: {},
      reactionsByChirp: {},
    },
    [serializedMockLaoId]: {
      allIdsInOrder: [
        chirp4.id.toState(),
        chirp2.id.toState(),
        chirp3.id.toState(),
        chirp1.id.toState(),
      ],
      byId: {
        [chirp1.id.toState()]: chirp1.toState(),
        [chirp2.id.toState()]: chirp2.toState(),
        [chirp3.id.toState()]: chirp3.toState(),
        [chirp4.id.toState()]: chirp4Deleted.toState(),
      },
      byUser: {
        [chirp1.sender.toState()]: [chirp4.id.toState(), chirp3.id.toState(), chirp1.id.toState()],
        [chirp2.sender.toState()]: [chirp2.id.toState()],
      },
      reactionsByChirp: {},
    },
  },
};

const reactionFilledState1: SocialLaoReducerState = {
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

const reactionFilledState11: SocialLaoReducerState = {
  byLaoId: {
    myLaoId: {
      allIdsInOrder: [],
      byId: {},
      byUser: {},
      reactionsByChirp: {},
    },
    [serializedMockLaoId]: {
      allIdsInOrder: [chirp1.id.toState()],
      byId: { [chirp1.id.toState()]: chirp1.toState() },
      byUser: { [chirp1.sender.toState()]: [chirp1.id.toState()] },
      reactionsByChirp: { [mockChirpId1.toState()]: { '👍': [mockSender1.toState()] } },
    },
  },
};

const reactionFilledState2: SocialLaoReducerState = {
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
        [mockChirpId1.toState()]: {
          '👍': [mockSender1.toState()],
          '❤️': [mockSender1.toState()],
        },
      },
    },
  },
};

const reactionFilledState22: SocialLaoReducerState = {
  byLaoId: {
    myLaoId: {
      allIdsInOrder: [],
      byId: {},
      byUser: {},
      reactionsByChirp: {},
    },
    [serializedMockLaoId]: {
      allIdsInOrder: [chirp1.id.toState()],
      byId: { [chirp1.id.toState()]: chirp1.toState() },
      byUser: { [chirp1.sender.toState()]: [chirp1.id.toState()] },
      reactionsByChirp: {
        [mockChirpId1.toState()]: {
          '👍': [mockSender1.toState()],
          '❤️': [mockSender1.toState()],
        },
      },
    },
  },
};

const reactionFilledState3: SocialLaoReducerState = {
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
        [mockChirpId1.toState()]: {
          '👍': [mockSender1.toState(), mockSender2.toState()],
          '❤️': [mockSender1.toState()],
        },
      },
    },
  },
};

const reactionFilledState33: SocialLaoReducerState = {
  byLaoId: {
    myLaoId: {
      allIdsInOrder: [],
      byId: {},
      byUser: {},
      reactionsByChirp: {},
    },
    [serializedMockLaoId]: {
      allIdsInOrder: [chirp1.id.toState()],
      byId: { [chirp1.id.toState()]: chirp1.toState() },
      byUser: { [chirp1.sender.toState()]: [chirp1.id.toState()] },
      reactionsByChirp: {
        [mockChirpId1.toState()]: {
          '👍': [mockSender1.toState(), mockSender2.toState()],
          '❤️': [mockSender1.toState()],
        },
      },
    },
  },
};

const reactionFilledState4: SocialLaoReducerState = {
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
        [mockChirpId1.toState()]: { '👍': [mockSender1.toState()] },
        [mockChirpId2.toState()]: { '👍': [mockSender2.toState()] },
      },
    },
  },
};

const reactionFilledState44: SocialLaoReducerState = {
  byLaoId: {
    myLaoId: {
      allIdsInOrder: [],
      byId: {},
      byUser: {},
      reactionsByChirp: {},
    },
    [serializedMockLaoId]: {
      allIdsInOrder: [chirp2.id.toState(), chirp1.id.toState()],
      byId: {
        [chirp1.id.toState()]: chirp1.toState(),
        [chirp2.id.toState()]: chirp2.toState(),
      },
      byUser: {
        [chirp1.sender.toState()]: [chirp1.id.toState()],
        [chirp2.sender.toState()]: [chirp2.id.toState()],
      },
      reactionsByChirp: {
        [mockChirpId1.toState()]: { '👍': [mockSender1.toState()] },
        [mockChirpId2.toState()]: { '👍': [mockSender2.toState()] },
      },
    },
  },
};
// endregion

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
      expect(makeChirpsListOfUser(mockLaoId)(chirp1.sender).resultFunc(chirpFilledState3)).toEqual([
        chirp3,
        chirp1,
      ]);
    });

    it('should return an empty list for an inactive user', () => {
      expect(makeChirpsListOfUser(mockLaoId)(chirp2.sender).resultFunc(chirpFilledState1)).toEqual(
        [],
      );
    });

    it('should return an empty list for an undefined lao', () => {
      expect(makeChirpsListOfUser(mockLaoId)(chirp2.sender).resultFunc(chirpFilledState1)).toEqual(
        [],
      );
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
