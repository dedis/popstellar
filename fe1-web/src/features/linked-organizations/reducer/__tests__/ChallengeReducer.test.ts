import { describe } from '@jest/globals';
import { AnyAction } from 'redux';

import { mockLao, mockLaoId, mockLaoId2, serializedMockLaoId } from '__tests__/utils';
import { Hash, Timestamp } from 'core/objects';
import { Challenge, ChallengeState } from 'features/linked-organizations/objects/Challenge';
import { DAY_IN_SECONDS } from 'resources/const';

import {
  setChallenge,
  CHALLENGE_REDUCER_PATH,
  challengeReduce,
  ChallengeReducerState,
  makeChallengeSelector,
  addReceivedChallenge,
  removeReceivedChallenge,
  makeChallengeReceveidSelector,
} from '../ChallengeReducer';

const mockChallenge: Challenge = new Challenge({
  value: new Hash('82520f235f413b26571529f69d53d751335873efca97e15cd7c47d063ead830d'),
  valid_until: Timestamp.EpochNow().addSeconds(DAY_IN_SECONDS),
});

const mockChallengeState: ChallengeState = mockChallenge.toState();

describe('ChallengeReducer', () => {
  describe('returns a valid initial state', () => {
    it('returns a valid initial state', () => {
      expect(challengeReduce(undefined, {} as AnyAction)).toEqual({
        byLaoId: {},
        recvChallenges: {},
      } as ChallengeReducerState);
    });
  });

  describe('setChallenge', () => {
    it('sets new challenge to the state', () => {
      const newState = challengeReduce(
        {
          byLaoId: {},
          recvChallenges: {},
        } as ChallengeReducerState,
        setChallenge(mockLaoId, mockChallengeState),
      );
      expect(newState.byLaoId[serializedMockLaoId]).toEqual(mockChallengeState);
    });
  });

  describe('addReceivedChallenge', () => {
    it('adds new received challenge (from fed-result) to the state', () => {
      const newState = challengeReduce(
        {
          byLaoId: {},
          recvChallenges: {},
        } as ChallengeReducerState,
        addReceivedChallenge(mockLaoId, mockChallengeState, mockLao.organizer),
      );
      expect(
        newState.recvChallenges[mockLaoId.valueOf()]?.find(
          ([challenge]) =>
            challenge.value === mockChallengeState.value &&
            challenge.valid_until === mockChallengeState.valid_until,
        ),
      ).toBeTruthy();
    });
  });

  describe('removeReceivedChallenge', () => {
    it('removes new received challenge (from fed-result) from the state', () => {
      let newState = challengeReduce(
        {
          byLaoId: {},
          recvChallenges: {},
        } as ChallengeReducerState,
        addReceivedChallenge(mockLaoId, mockChallengeState, mockLao.organizer),
      );
      newState = challengeReduce(
        newState,
        removeReceivedChallenge(mockLaoId, mockChallengeState, mockLao.organizer),
      );
      expect(
        newState.recvChallenges[mockLaoId.valueOf()]?.find(
          ([challenge]) =>
            challenge.value === mockChallengeState.value &&
            challenge.valid_until === mockChallengeState.valid_until,
        ),
      ).toBeUndefined();
    });
  });
});

describe('makeChallengeSelector', () => {
  it('returns the correct challenge', () => {
    const newState = challengeReduce(
      {
        byLaoId: {},
        recvChallenges: {},
      } as ChallengeReducerState,
      setChallenge(mockLaoId, mockChallengeState),
    );
    expect(newState.byLaoId[serializedMockLaoId]).toEqual(mockChallengeState);
    expect(
      makeChallengeSelector(mockLaoId)({
        [CHALLENGE_REDUCER_PATH]: {
          byLaoId: { [serializedMockLaoId]: mockChallengeState },
        } as ChallengeReducerState,
      }),
    ).toEqual(mockChallengeState);
  });

  it('returns undefined if the laoId is not in the store', () => {
    const newState = challengeReduce(
      {
        byLaoId: {},
        recvChallenges: {},
      } as ChallengeReducerState,
      setChallenge(mockLaoId, mockChallengeState),
    );
    expect(newState.byLaoId[serializedMockLaoId]).toEqual(mockChallengeState);
    expect(
      makeChallengeSelector(mockLaoId2)({
        [CHALLENGE_REDUCER_PATH]: {
          byLaoId: { [serializedMockLaoId]: mockChallengeState },
        } as ChallengeReducerState,
      }),
    ).toBeUndefined();
  });
});

describe('makeChallengeReceveidSelector', () => {
  it('returns the correct challenge(s)', () => {
    const newState = challengeReduce(
      {
        byLaoId: {},
        recvChallenges: {},
      } as ChallengeReducerState,
      addReceivedChallenge(mockLaoId, mockChallengeState, mockLao.organizer),
    );
    expect(
      newState.recvChallenges[mockLaoId.valueOf()]?.find(
        ([challenge]) =>
          challenge.value.valueOf() === mockChallengeState.value.valueOf() &&
          challenge.valid_until.valueOf() === mockChallengeState.valid_until.valueOf(),
      ),
    ).toBeTruthy();
    expect(
      makeChallengeReceveidSelector(mockLaoId)({
        [CHALLENGE_REDUCER_PATH]: {
          byLaoId: {},
          recvChallenges: { [serializedMockLaoId]: [[mockChallengeState, mockLao.organizer]] },
        } as ChallengeReducerState,
      }),
    ).toEqual([[mockChallengeState, mockLao.organizer]]);
  });

  it('returns undefined if the laoId is not in the store', () => {
    const newState = challengeReduce(
      {
        byLaoId: {},
        recvChallenges: {},
      } as ChallengeReducerState,
      addReceivedChallenge(mockLaoId, mockChallengeState, mockLao.organizer),
    );
    expect(
      newState.recvChallenges[mockLaoId.valueOf()]?.find(
        ([challenge]) =>
          challenge.value.valueOf() === mockChallengeState.value.valueOf() &&
          challenge.valid_until.valueOf() === mockChallengeState.valid_until.valueOf(),
      ),
    ).toBeTruthy();
    expect(
      makeChallengeReceveidSelector(mockLaoId2)({
        [CHALLENGE_REDUCER_PATH]: {
          byLaoId: {},
          recvChallenges: { [serializedMockLaoId]: [[mockChallengeState, mockLao.organizer]] },
        } as ChallengeReducerState,
      }),
    ).toBeUndefined();
  });
});
