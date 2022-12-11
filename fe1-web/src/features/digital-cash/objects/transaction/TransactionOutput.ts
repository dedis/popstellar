import { Hash, HashState } from 'core/objects';

export interface TransactionOutputJSON {
  value: number;
  script: TransactionOutputScriptJSON;
}
export interface TransactionOutputScriptJSON {
  type: string;
  pubkey_hash: string;
}

export interface TransactionOutputState {
  value: number;
  script: TransactionOutputScriptState;
}
export interface TransactionOutputScriptState {
  type: string;
  publicKeyHash: HashState;
}
export interface TransactionOutputScript {
  type: string;
  publicKeyHash: Hash;
}

/**
 * An output for a coin transaction object
 */
export class TransactionOutput {
  public readonly value: number;

  public readonly script: TransactionOutputScript;

  constructor(obj: Partial<TransactionOutput>) {
    if (obj === undefined || obj === null) {
      throw new Error(
        'Error encountered while creating a TransactionOutput object: undefined/null parameters',
      );
    }

    if (obj.value === undefined) {
      throw new Error("Undefined 'value' when creating 'TransactionOutput'");
    }
    this.value = obj.value;

    if (obj.script === undefined) {
      throw new Error("Undefined 'script' when creating 'TransactionOutput'");
    }
    this.script = obj.script;
  }

  public static fromState(state: TransactionOutputState) {
    return new TransactionOutput({
      ...state,
      script: {
        ...state.script,
        publicKeyHash: Hash.fromState(state.script.publicKeyHash),
      },
    });
  }

  public toState(): TransactionOutputState {
    return {
      value: this.value,
      script: {
        type: this.script.type,
        publicKeyHash: this.script.publicKeyHash.toState(),
      },
    };
  }

  public static fromJSON(json: TransactionOutputJSON) {
    return new TransactionOutput({
      ...json,
      script: {
        type: json.script.type,
        publicKeyHash: new Hash(json.script.pubkey_hash),
      },
    });
  }

  public toJSON(): TransactionOutputJSON {
    return {
      ...this,
      script: {
        type: this.script.type,
        pubkey_hash: this.script.publicKeyHash.valueOf(),
      },
    };
  }
}
