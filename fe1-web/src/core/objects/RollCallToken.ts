/**
 * A Roll Call Token object, defined by a Pop token, its lao id hash and its Roll Call id hash
 */
import { OmitMethods } from 'core/types';

import { Hash } from './Hash';
import { PopToken } from './PopToken';

export class RollCallToken {
  public readonly token: PopToken;

  public readonly laoId: Hash;

  public readonly rollCallId: Hash;

  public readonly rollCallName: string;

  constructor(obj: OmitMethods<RollCallToken>) {
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
    if (obj.rollCallName === undefined) {
      throw new Error("Undefined 'rollCallName' when creating 'RollCallToken'");
    }

    this.token = obj.token;
    this.laoId = obj.laoId;
    this.rollCallId = obj.rollCallId;
    this.rollCallName = obj.rollCallName;
  }
}
