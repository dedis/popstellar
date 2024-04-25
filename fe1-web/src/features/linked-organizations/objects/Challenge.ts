import { Hash, HashState, Timestamp } from 'core/objects';
import { OmitMethods } from 'core/types';

export interface ChallengeState {
  value: HashState;
  valid_until: Timestamp;
}

export class Challenge {
  public readonly value: Hash;

  public readonly valid_until: Timestamp;


  constructor(challenge: OmitMethods<Challenge>) {
    this.value = challenge.value;
    this.valid_until = challenge.valid_until;
  }

  public toState(): ChallengeState {
    return {
      value: this.value.toState(),
      valid_until: this.valid_until,
    };
  }



  public static fromState(challengeState: ChallengeState): Challenge {
    return new Challenge({
      value: Hash.fromState(challengeState.value),
      valid_until: challengeState.valid_until,
    });
  }

  public static fromJson(obj: any): Challenge {
    return new Challenge({
      value: new Hash(obj.value),
      valid_until: obj.valid_until,
    });
  }

  public toJson(): string {
    return JSON.stringify({
      value: this.value,
      valid_until: this.valid_until.valueOf(),
    });
  }
}
