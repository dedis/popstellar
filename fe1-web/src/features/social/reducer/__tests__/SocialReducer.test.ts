import 'jest-extended';
import '__tests__/utils/matchers';

import { describe } from '@jest/globals';
import { AnyAction } from 'redux';

import { serializedMockLaoId, mockLaoId } from '__tests__/utils/TestUtils';
import {
  mockChirp0,
  mockChirp0Deleted,
  mockChirp1,
  mockChirp1Deleted,
  mockChirp1DeletedFake,
  mockChirp2,
  mockChirp3,
  mockChirp4,
  mockChirp4Deleted,
  mockChirpId0,
  mockChirpId1,
  mockChirpId2,
  mockChirpId3,
  mockReaction1,
  mockReaction2,
  mockReaction3,
  mockReaction4,
  mockReaction5,
  mockReaction6,
  mockSender1,
} from 'features/social/__tests__/utils';

import {
  addChirp,
  addReaction,
  deleteChirp,
  makeChirpsList,
  makeChirpsListOfUser,
  makeReactedSelector,
  makeReactionCountsSelector,
  makeTopChirpsSelector,
  SCORE_BY_CODE_POINT,
  SocialLaoReducerState,
  socialReduce,
} from '../SocialReducer';

// region test data

const emptyState: SocialLaoReducerState = {
  byLaoId: {},
};

const chirpFilledState0Deleted: SocialLaoReducerState = {
  byLaoId: {
    [serializedMockLaoId]: {
      allIdsInOrder: [],
      byId: { [mockChirpId0.toState()]: mockChirp0Deleted.toState() },
      byUser: {},
      scoreByChirpId: {},
      reactionsByChirpId: {},
      reactionsById: {},
    },
  },
};

const chirpFilledState0Added: SocialLaoReducerState = {
  byLaoId: {
    [serializedMockLaoId]: {
      allIdsInOrder: [mockChirp0.id.toState()],
      byId: { [mockChirp0.id.toState()]: mockChirp0.toState() },
      byUser: { [mockChirp0.sender.toState()]: [mockChirp0.id.toState()] },
      scoreByChirpId: { [mockChirp0.id.toState()]: 0 },
      reactionsByChirpId: {},
      reactionsById: {},
    },
  },
};

const chirpFilledState1: SocialLaoReducerState = {
  byLaoId: {
    [serializedMockLaoId]: {
      allIdsInOrder: [mockChirp1.id.toState()],
      byId: { [mockChirp1.id.toState()]: mockChirp1.toState() },
      byUser: { [mockChirp1.sender.toState()]: [mockChirp1.id.toState()] },
      scoreByChirpId: { [mockChirp1.id.toState()]: 0 },
      reactionsByChirpId: {},
      reactionsById: {},
    },
  },
};

const chirpFilledState2: SocialLaoReducerState = {
  byLaoId: {
    [serializedMockLaoId]: {
      allIdsInOrder: [mockChirp2.id.toState(), mockChirp1.id.toState()],
      byId: {
        [mockChirp1.id.toState()]: mockChirp1.toState(),
        [mockChirp2.id.toState()]: mockChirp2.toState(),
      },
      byUser: {
        [mockChirp1.sender.toState()]: [mockChirp1.id.toState()],
        [mockChirp2.sender.toState()]: [mockChirp2.id.toState()],
      },
      scoreByChirpId: {
        [mockChirp1.id.toState()]: 0,
        [mockChirp2.id.toState()]: 0,
      },
      reactionsByChirpId: {},
      reactionsById: {},
    },
  },
};

const chirpFilledState3: SocialLaoReducerState = {
  byLaoId: {
    [serializedMockLaoId]: {
      allIdsInOrder: [mockChirp2.id.toState(), mockChirp3.id.toState(), mockChirp1.id.toState()],
      byId: {
        [mockChirp1.id.toState()]: mockChirp1.toState(),
        [mockChirp2.id.toState()]: mockChirp2.toState(),
        [mockChirp3.id.toState()]: mockChirp3.toState(),
      },
      byUser: {
        [mockChirp1.sender.toState()]: [mockChirp3.id.toState(), mockChirp1.id.toState()],
        [mockChirp2.sender.toState()]: [mockChirp2.id.toState()],
      },
      scoreByChirpId: {
        [mockChirp1.id.toState()]: 0,
        [mockChirp2.id.toState()]: 0,
        [mockChirp3.id.toState()]: 0,
      },
      reactionsByChirpId: {},
      reactionsById: {},
    },
  },
};

const chirpFilledState4: SocialLaoReducerState = {
  byLaoId: {
    [serializedMockLaoId]: {
      allIdsInOrder: [
        mockChirp4.id.toState(),
        mockChirp2.id.toState(),
        mockChirp3.id.toState(),
        mockChirp1.id.toState(),
      ],
      byId: {
        [mockChirp1.id.toState()]: mockChirp1.toState(),
        [mockChirp2.id.toState()]: mockChirp2.toState(),
        [mockChirp3.id.toState()]: mockChirp3.toState(),
        [mockChirp4.id.toState()]: mockChirp4.toState(),
      },
      byUser: {
        [mockChirp1.sender.toState()]: [
          mockChirp4.id.toState(),
          mockChirp3.id.toState(),
          mockChirp1.id.toState(),
        ],
        [mockChirp2.sender.toState()]: [mockChirp2.id.toState()],
      },
      scoreByChirpId: {
        [mockChirp1.id.toState()]: 0,
        [mockChirp2.id.toState()]: 0,
        [mockChirp3.id.toState()]: 0,
        [mockChirp4.id.toState()]: 0,
      },
      reactionsByChirpId: {},
      reactionsById: {},
    },
  },
};

const chirpFilledState4Chirp1Deleted: SocialLaoReducerState = {
  byLaoId: {
    [serializedMockLaoId]: {
      allIdsInOrder: [
        mockChirp4.id.toState(),
        mockChirp2.id.toState(),
        mockChirp3.id.toState(),
        mockChirp1.id.toState(),
      ],
      byId: {
        [mockChirp1.id.toState()]: mockChirp1Deleted.toState(),
        [mockChirp2.id.toState()]: mockChirp2.toState(),
        [mockChirp3.id.toState()]: mockChirp3.toState(),
        [mockChirp4.id.toState()]: mockChirp4.toState(),
      },
      byUser: {
        [mockChirp1.sender.toState()]: [
          mockChirp4.id.toState(),
          mockChirp3.id.toState(),
          mockChirp1.id.toState(),
        ],
        [mockChirp2.sender.toState()]: [mockChirp2.id.toState()],
      },
      scoreByChirpId: {
        [mockChirp1.id.toState()]: 0,
        [mockChirp2.id.toState()]: 0,
        [mockChirp3.id.toState()]: 0,
        [mockChirp4.id.toState()]: 0,
      },
      reactionsByChirpId: {},
      reactionsById: {},
    },
  },
};

const chirpFilledState4Chirp4Deleted: SocialLaoReducerState = {
  byLaoId: {
    [serializedMockLaoId]: {
      allIdsInOrder: [
        mockChirp4.id.toState(),
        mockChirp2.id.toState(),
        mockChirp3.id.toState(),
        mockChirp1.id.toState(),
      ],
      byId: {
        [mockChirp1.id.toState()]: mockChirp1.toState(),
        [mockChirp2.id.toState()]: mockChirp2.toState(),
        [mockChirp3.id.toState()]: mockChirp3.toState(),
        [mockChirp4.id.toState()]: mockChirp4Deleted.toState(),
      },
      byUser: {
        [mockChirp1.sender.toState()]: [
          mockChirp4.id.toState(),
          mockChirp3.id.toState(),
          mockChirp1.id.toState(),
        ],
        [mockChirp2.sender.toState()]: [mockChirp2.id.toState()],
      },
      scoreByChirpId: {
        [mockChirp1.id.toState()]: 0,
        [mockChirp2.id.toState()]: 0,
        [mockChirp3.id.toState()]: 0,
        [mockChirp4.id.toState()]: 0,
      },
      reactionsByChirpId: {},
      reactionsById: {},
    },
  },
};

const reactionFilledState1: SocialLaoReducerState = {
  byLaoId: {
    [serializedMockLaoId]: {
      allIdsInOrder: [],
      byId: {},
      byUser: {},
      scoreByChirpId: { [mockChirpId1.toState()]: SCORE_BY_CODE_POINT[mockReaction1.codepoint] },
      reactionsByChirpId: { [mockChirpId1.toState()]: [mockReaction1.id.toState()] },
      reactionsById: { [mockReaction1.id.toState()]: mockReaction1.toState() },
    },
  },
};

const reactionFilledState11: SocialLaoReducerState = {
  byLaoId: {
    [serializedMockLaoId]: {
      allIdsInOrder: [mockChirp1.id.toState()],
      byId: { [mockChirp1.id.toState()]: mockChirp1.toState() },
      byUser: { [mockChirp1.sender.toState()]: [mockChirp1.id.toState()] },
      scoreByChirpId: { [mockChirpId1.toState()]: SCORE_BY_CODE_POINT[mockReaction1.codepoint] },
      reactionsByChirpId: { [mockChirpId1.toState()]: [mockReaction1.id.toState()] },
      reactionsById: { [mockReaction1.id.toState()]: mockReaction1.toState() },
    },
  },
};

const reactionFilledState2: SocialLaoReducerState = {
  byLaoId: {
    [serializedMockLaoId]: {
      allIdsInOrder: [],
      byId: {},
      byUser: {},
      scoreByChirpId: {
        [mockChirpId1.toState()]:
          SCORE_BY_CODE_POINT[mockReaction1.codepoint] +
          SCORE_BY_CODE_POINT[mockReaction2.codepoint],
      },
      reactionsByChirpId: {
        [mockChirpId1.toState()]: [mockReaction1.id.toState(), mockReaction2.id.toState()],
      },
      reactionsById: {
        [mockReaction1.id.toState()]: mockReaction1.toState(),
        [mockReaction2.id.toState()]: mockReaction2.toState(),
      },
    },
  },
};

const reactionFilledState22: SocialLaoReducerState = {
  byLaoId: {
    [serializedMockLaoId]: {
      allIdsInOrder: [mockChirp1.id.toState()],
      byId: { [mockChirp1.id.toState()]: mockChirp1.toState() },
      byUser: { [mockChirp1.sender.toState()]: [mockChirp1.id.toState()] },
      scoreByChirpId: {
        [mockChirpId1.toState()]:
          SCORE_BY_CODE_POINT[mockReaction1.codepoint] +
          SCORE_BY_CODE_POINT[mockReaction2.codepoint],
      },
      reactionsByChirpId: {
        [mockChirpId1.toState()]: [mockReaction1.id.toState(), mockReaction2.id.toState()],
      },
      reactionsById: {
        [mockReaction1.id.toState()]: mockReaction1.toState(),
        [mockReaction2.id.toState()]: mockReaction2.toState(),
      },
    },
  },
};

const reactionFilledState3: SocialLaoReducerState = {
  byLaoId: {
    [serializedMockLaoId]: {
      allIdsInOrder: [],
      byId: {},
      byUser: {},
      scoreByChirpId: {
        [mockChirpId1.toState()]:
          SCORE_BY_CODE_POINT[mockReaction1.codepoint] +
          SCORE_BY_CODE_POINT[mockReaction2.codepoint] +
          SCORE_BY_CODE_POINT[mockReaction3.codepoint],
      },
      reactionsByChirpId: {
        [mockChirpId1.toState()]: [
          mockReaction1.id.toState(),
          mockReaction2.id.toState(),
          mockReaction3.id.toState(),
        ],
      },
      reactionsById: {
        [mockReaction1.id.toState()]: mockReaction1.toState(),
        [mockReaction2.id.toState()]: mockReaction2.toState(),
        [mockReaction3.id.toState()]: mockReaction3.toState(),
      },
    },
  },
};

const reactionFilledState33: SocialLaoReducerState = {
  byLaoId: {
    [serializedMockLaoId]: {
      allIdsInOrder: [mockChirp1.id.toState()],
      byId: { [mockChirp1.id.toState()]: mockChirp1.toState() },
      byUser: { [mockChirp1.sender.toState()]: [mockChirp1.id.toState()] },
      scoreByChirpId: {
        [mockChirpId1.toState()]:
          SCORE_BY_CODE_POINT[mockReaction1.codepoint] +
          SCORE_BY_CODE_POINT[mockReaction2.codepoint] +
          SCORE_BY_CODE_POINT[mockReaction3.codepoint],
      },
      reactionsByChirpId: {
        [mockChirpId1.toState()]: [
          mockReaction1.id.toState(),
          mockReaction2.id.toState(),
          mockReaction3.id.toState(),
        ],
      },
      reactionsById: {
        [mockReaction1.id.toState()]: mockReaction1.toState(),
        [mockReaction2.id.toState()]: mockReaction2.toState(),
        [mockReaction3.id.toState()]: mockReaction3.toState(),
      },
    },
  },
};

const reactionFilledState4: SocialLaoReducerState = {
  byLaoId: {
    [serializedMockLaoId]: {
      allIdsInOrder: [],
      byId: {},
      byUser: {},
      scoreByChirpId: {
        [mockChirpId1.toState()]: SCORE_BY_CODE_POINT[mockReaction1.codepoint],
        [mockChirpId2.toState()]: SCORE_BY_CODE_POINT[mockReaction4.codepoint],
      },
      reactionsByChirpId: {
        [mockChirpId1.toState()]: [mockReaction1.id.toState()],
        [mockChirpId2.toState()]: [mockReaction4.id.toState()],
      },
      reactionsById: {
        [mockReaction1.id.toState()]: mockReaction1.toState(),
        [mockReaction4.id.toState()]: mockReaction4.toState(),
      },
    },
  },
};

const reactionFilledState5: SocialLaoReducerState = {
  byLaoId: {
    [serializedMockLaoId]: {
      allIdsInOrder: [mockChirp1.id.toState(), mockChirp2.id.toState()],
      byId: {
        [mockChirp1.id.toState()]: mockChirp1.toState(),
        [mockChirp2.id.toState()]: mockChirp2.toState(),
        [mockChirp3.id.toState()]: { ...mockChirp3.toState(), isDeleted: true },
      },
      byUser: {
        [mockChirp1.sender.toState()]: [mockChirp1.id.toState(), mockChirp3.id.toState()],
        [mockChirp2.sender.toState()]: [mockChirp2.id.toState()],
      },
      scoreByChirpId: {
        [mockChirpId1.toState()]:
          SCORE_BY_CODE_POINT[mockReaction1.codepoint] +
          SCORE_BY_CODE_POINT[mockReaction5.codepoint],
        [mockChirpId2.toState()]: SCORE_BY_CODE_POINT[mockReaction4.codepoint],
        [mockChirpId3.toState()]: SCORE_BY_CODE_POINT[mockReaction6.codepoint],
      },
      reactionsByChirpId: {
        [mockChirpId1.toState()]: [mockReaction1.id.toState(), mockReaction5.id.toState()],
        [mockChirpId2.toState()]: [mockReaction4.id.toState()],
        [mockChirpId3.toState()]: [mockReaction6.id.toState()],
      },
      reactionsById: {
        [mockReaction1.id.toState()]: mockReaction1.toState(),
        [mockReaction4.id.toState()]: mockReaction4.toState(),
        [mockReaction5.id.toState()]: mockReaction5.toState(),
        [mockReaction6.id.toState()]: mockReaction6.toState(),
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
      expect(socialReduce(emptyState, addChirp(mockLaoId, mockChirp1))).toEqual(chirpFilledState1);
    });

    it('should add the newer chirp before the first chirp', () => {
      expect(socialReduce(chirpFilledState1, addChirp(mockLaoId, mockChirp2))).toEqual(
        chirpFilledState2,
      );
    });

    it('should add the newer chirp after the second chirp', () => {
      expect(socialReduce(chirpFilledState2, addChirp(mockLaoId, mockChirp3))).toEqual(
        chirpFilledState3,
      );
    });

    it('should add the newest chirp on top', () => {
      expect(socialReduce(chirpFilledState3, addChirp(mockLaoId, mockChirp4))).toEqual(
        chirpFilledState4,
      );
    });

    it('should mark chirp 1 as deleted', () => {
      expect(socialReduce(chirpFilledState4, deleteChirp(mockLaoId, mockChirp1Deleted))).toEqual(
        chirpFilledState4Chirp1Deleted,
      );
    });

    it('delete a non-stored chirp should store it in byId as deleted', () => {
      expect(socialReduce(emptyState, deleteChirp(mockLaoId, mockChirp0Deleted))).toEqual(
        chirpFilledState0Deleted,
      );
    });

    it('should ignore delete request sent by non-original sender', () => {
      expect(
        socialReduce(chirpFilledState4, deleteChirp(mockLaoId, mockChirp1DeletedFake)),
      ).toEqual(chirpFilledState4);
    });

    it('should update/add a chirp if it has been deleted by a different sender', () => {
      expect(socialReduce(chirpFilledState0Deleted, addChirp(mockLaoId, mockChirp0))).toEqual(
        chirpFilledState0Added,
      );
    });

    it('should not re-add a chirp if it has already been deleted by the same sender', () => {
      const stateDeleted = socialReduce(chirpFilledState3, deleteChirp(mockLaoId, mockChirp4));
      expect(socialReduce(stateDeleted, addChirp(mockLaoId, mockChirp4))).toEqual(
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
      expect(makeChirpsList(mockLaoId).resultFunc(chirpFilledState1)).toEqual([mockChirp1]);
    });

    it('should return the newer chirp before the first chirp', () => {
      expect(makeChirpsList(mockLaoId).resultFunc(chirpFilledState2)).toEqual([
        mockChirp2,
        mockChirp1,
      ]);
    });

    it('should add the newer chirp after the second chirp', () => {
      expect(makeChirpsList(mockLaoId).resultFunc(chirpFilledState3)).toEqual([
        mockChirp2,
        mockChirp3,
        mockChirp1,
      ]);
    });

    it('should return the newest chirp on top', () => {
      expect(makeChirpsList(mockLaoId).resultFunc(chirpFilledState4)).toEqual([
        mockChirp4,
        mockChirp2,
        mockChirp3,
        mockChirp1,
      ]);
    });

    it('should return the correct chirps list for an active user', () => {
      expect(
        makeChirpsListOfUser(mockLaoId)(mockChirp1.sender).resultFunc(chirpFilledState3),
      ).toEqual([mockChirp3, mockChirp1]);
    });

    it('should return an empty list for an inactive user', () => {
      expect(
        makeChirpsListOfUser(mockLaoId)(mockChirp2.sender).resultFunc(chirpFilledState1),
      ).toEqual([]);
    });

    it('should return an empty list for an undefined lao', () => {
      expect(
        makeChirpsListOfUser(mockLaoId)(mockChirp2.sender).resultFunc(chirpFilledState1),
      ).toEqual([]);
    });
  });

  describe('reaction reducer', () => {
    it('should create entry for a chirp when receiving the first reaction on it', () => {
      console.error(emptyState);
      console.error(
        socialReduce(
          {
            byLaoId: {},
          },
          addReaction(mockLaoId, mockReaction1),
        ).byLaoId[mockLaoId.toState()].allIdsInOrder,
      );
      expect(socialReduce(emptyState, addReaction(mockLaoId, mockReaction1))).toBeJsonEqual(
        reactionFilledState1,
      );
    });

    it('should add reaction codepoint to an existing chirp', () => {
      expect(
        socialReduce(reactionFilledState1, addReaction(mockLaoId, mockReaction2)),
      ).toBeJsonEqual(reactionFilledState2);
    });

    it('should add new reaction sender for a chirp', () => {
      expect(
        socialReduce(reactionFilledState2, addReaction(mockLaoId, mockReaction3)),
      ).toBeJsonEqual(reactionFilledState3);
    });

    it('should not add existing sender of a reaction for a chirp', () => {
      expect(
        socialReduce(reactionFilledState3, addReaction(mockLaoId, mockReaction1)),
      ).toBeJsonEqual(reactionFilledState3);
    });

    it('should create new chirp entry correctly', () => {
      expect(
        socialReduce(reactionFilledState1, addReaction(mockLaoId, mockReaction4)),
      ).toBeJsonEqual(reactionFilledState4);
    });
  });

  describe('makeReactionCountsSelector', () => {
    it('should return zeros for an empty state', () => {
      expect(makeReactionCountsSelector(mockLaoId, mockChirpId1).resultFunc(emptyState)).toEqual({
        '👍': 0,
        '👎': 0,
        '❤️': 0,
      });
    });

    it('should return zeros for non-stored chirp', () => {
      expect(
        makeReactionCountsSelector(mockLaoId, mockChirpId2).resultFunc(reactionFilledState1),
      ).toEqual({ '👍': 0, '👎': 0, '❤️': 0 });
    });

    it('should return the correct counts', () => {
      expect(
        makeReactionCountsSelector(mockLaoId, mockChirpId1).resultFunc(reactionFilledState11),
      ).toEqual({
        '👍': 1,
        '👎': 0,
        '❤️': 0,
      });
    });

    it('should add reaction count correctly', () => {
      expect(
        makeReactionCountsSelector(mockLaoId, mockChirpId1).resultFunc(reactionFilledState22),
      ).toEqual({
        '👍': 1,
        '👎': 0,
        '❤️': 1,
      });
    });

    it('should increment counter for new sender', () => {
      expect(
        makeReactionCountsSelector(mockLaoId, mockChirpId1).resultFunc(reactionFilledState33),
      ).toEqual({
        '👍': 2,
        '👎': 0,
        '❤️': 1,
      });
    });
  });

  describe('makeHasReactedSelector', () => {
    it('should return an empty object for an empty state', () => {
      expect(
        makeReactedSelector(mockLaoId, mockChirpId1, mockSender1).resultFunc(emptyState),
      ).toEqual({});
    });

    it('should return empty object for chirp without reactions', () => {
      expect(
        makeReactedSelector(mockLaoId, mockChirpId2, mockSender1).resultFunc(reactionFilledState1),
      ).toEqual({});
    });

    it('should return the reacted state correctly', () => {
      expect(
        makeReactedSelector(mockLaoId, mockChirpId1, mockSender1).resultFunc(reactionFilledState11),
      ).toEqual({
        '👍': mockReaction1,
      });
    });
  });

  describe('makeTopChirpsSelector', () => {
    it('should return an empty list for an empty state', () => {
      expect(makeTopChirpsSelector(mockLaoId, 3).resultFunc(emptyState)).toEqual([]);
    });

    it('should return the chirps in the correct order', () => {
      expect(makeTopChirpsSelector(mockLaoId, 2).resultFunc(reactionFilledState5)).toEqual([
        mockChirp2,
        mockChirp1,
      ]);
    });

    it("should return at most 'max' chirps", () => {
      expect(makeTopChirpsSelector(mockLaoId, 1).resultFunc(reactionFilledState5)).toEqual([
        mockChirp2,
      ]);
    });

    it('should omit deleted chirps', () => {
      expect(makeTopChirpsSelector(mockLaoId, 3).resultFunc(reactionFilledState5)).toEqual([
        mockChirp2,
        mockChirp1,
      ]);
    });
  });
});
