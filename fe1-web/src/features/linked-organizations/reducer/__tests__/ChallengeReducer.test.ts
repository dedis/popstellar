import { describe } from '@jest/globals';
import { AnyAction } from 'redux';

import { mockLaoId, mockLaoId2, mockPublicKey, serializedMockLaoId } from '__tests__/utils';
import { Hash, Timestamp } from 'core/objects';
import { Challenge } from 'features/linked-organizations/objects/Challenge';
import { ChallengeState } from 'features/linked-organizations/objects/Challenge';
import { addChallenge, CHALLENGE_REDUCER_PATH, challengeReduce, ChallengeReducerState, makeChallengeSelector } from '../ChallengeReducer';


const mockChallenge: Challenge = new Challenge({
  value: new Hash('82520f235f413b26571529f69d53d751335873efca97e15cd7c47d063ead830d'),
  valid_until: Timestamp.EpochNow().addSeconds(86400),
});

const mockChallengeState: ChallengeState = mockChallenge.toState();

describe('ChallengeReducer', () => {
  describe('returns a valid initial state', () => {
    it('returns a valid initial state', () => {
      expect(challengeReduce(undefined, {} as AnyAction)).toEqual({
        byLaoId: {},
      } as ChallengeReducerState);
    });
  });

  describe('addChallenge', () => {
    it('adds new challenge to the state', () => {
      const newState = challengeReduce(
        {
          byLaoId: {},
        } as ChallengeReducerState,
        addChallenge(mockLaoId, mockChallengeState),
      );
      expect(newState.byLaoId[serializedMockLaoId]).toEqual(mockChallengeState);
    });

  });
});

describe('makeChallengeSelector', () => {
  it('returns the correct challenge', () => {
    const newState = challengeReduce(
      {
        byLaoId: {},
      } as ChallengeReducerState,
      addChallenge(mockLaoId, mockChallengeState),
    );
    expect(newState.byLaoId[serializedMockLaoId]).toEqual(mockChallengeState);
    expect(
      makeChallengeSelector(
        mockLaoId
      )({
        [CHALLENGE_REDUCER_PATH]: {
          byLaoId: { [serializedMockLaoId]: mockChallengeState }
        } as ChallengeReducerState,
      }),
    ).toEqual(mockChallengeState);
  });

  it('returns undefined if the laoId is not in the store', () => {
    const newState = challengeReduce(
      {
        byLaoId: {},
      } as ChallengeReducerState,
      addChallenge(mockLaoId, mockChallengeState),
    );
    expect(newState.byLaoId[serializedMockLaoId]).toEqual(mockChallengeState);
    expect(
      makeChallengeSelector(
        mockLaoId2
      )({
        [CHALLENGE_REDUCER_PATH]: {
          byLaoId: { [serializedMockLaoId]: mockChallengeState }
        } as ChallengeReducerState,
      }),
    ).toBeUndefined();
  });
});
