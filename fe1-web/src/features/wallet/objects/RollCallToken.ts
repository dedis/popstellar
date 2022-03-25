import { Hash, KeyPairState, PopToken } from 'core/objects';

export interface RollCallTokenState {
  token: KeyPairState;
  laoId: string;
  rollCallId: string;
}

export class RollCallToken {
  public readonly token: PopToken;

  public readonly laoId: Hash;

  public readonly rollCallId: Hash;

  constructor(obj: Partial<RollCallToken>) {
    if (obj === undefined || obj === null) {
      throw new Error(
        'Error encountered while creating a RollCallToken object: undefined/null parameters',
      );
    }

    if (obj.token === undefined) {
      throw new Error("Undefined 'token' when creating 'RollCallToken'");
    }
    if (obj.laoId === undefined) {
      throw new Error("Undefined 'laoId' when creating 'RollCallToken'");
    }
    if (obj.rollCallId === undefined) {
      throw new Error("Undefined 'rollCallId' when creating 'RollCallToken'");
    }

    this.token = obj.token;
    this.laoId = obj.laoId;
    this.rollCallId = obj.rollCallId;
  }

  public static fromState(rct: RollCallTokenState): RollCallToken {
    return new RollCallToken({
      token: PopToken.fromState(rct.token),
      laoId: new Hash(rct.laoId),
      rollCallId: new Hash(rct.rollCallId),
    });
  }

  public toState(): RollCallTokenState {
    return JSON.parse(JSON.stringify(this));
  }
}
