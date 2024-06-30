import { Hash, HashState, Timestamp } from 'core/objects';
import { OmitMethods } from 'core/types';

export interface ChallengeState {
  value: HashState;
  valid_until: number;
}

export class Challenge {
  public readonly value: Hash;

  public readonly valid_until: Timestamp;

  constructor(challenge: OmitMethods<Challenge>) {
    if (challenge === undefined || challenge === null) {
      throw new Error(
        'Error encountered while creating a Challenge object: undefined/null parameters',
      );
    }

    if (challenge.valid_until === undefined) {
      throw new Error("Undefined 'valid_until' when creating 'Challenge'");
    }
    if (challenge.value === undefined) {
      throw new Error("Undefined 'value' when creating 'Challenge'");
    }

    this.value = challenge.value;
    this.valid_until = challenge.valid_until;
  }

  public toState(): ChallengeState {
    return {
      value: this.value.toState(),
      valid_until: this.valid_until.valueOf(),
    };
  }

  public static fromState(challengeState: ChallengeState): Challenge {
    return new Challenge({
      value: Hash.fromState(challengeState.value),
      valid_until: new Timestamp(challengeState.valid_until),
    });
  }

  public static fromJson(obj: any): Challenge {
    return new Challenge({
      value: new Hash(obj.value),
      valid_until: new Timestamp(obj.valid_until),
    });
  }

  public toJson(): string {
    return JSON.stringify({
      value: this.value,
      valid_until: this.valid_until.valueOf(),
    });
  }
}
