import 'jest-extended';
import '__tests__/utils/matchers';

import { Hash, Timestamp } from 'core/objects';

import { Challenge, ChallengeState } from '../Challenge';

const VALID_HASH_VALUE = new Hash('valid_hash');
const VALID_TIMESTAMP = new Timestamp(123456789);

describe('Challenge object', () => {
  describe('state and JSON round trips', () => {
    it('does a state round trip correctly', () => {
      const challengeState: ChallengeState = {
        value: VALID_HASH_VALUE.toState(),
        valid_until: VALID_TIMESTAMP,
      };
      const challenge = Challenge.fromState(challengeState);
      expect(challenge.toState()).toStrictEqual(challengeState);
    });

    it('does a JSON round trip correctly', () => {
      const jsonObj = {
        value: VALID_HASH_VALUE.toString(),
        valid_until: VALID_TIMESTAMP.valueOf(),
      };
      const challenge = Challenge.fromJson(jsonObj);
      expect(JSON.parse(challenge.toJson())).toStrictEqual(jsonObj);
    });
  });

  describe('constructor', () => {
    it('throws an error when the object is undefined', () => {
      const createChallenge = () => new Challenge(undefined as unknown as Challenge);
      expect(createChallenge).toThrow(Error);
    });

    it('throws an error when the object is null', () => {
      const createChallenge = () => new Challenge(null as unknown as Challenge);
      expect(createChallenge).toThrow(Error);
    });
  });
});
