import {
  Hash,
  ProtocolError,
  PublicKey,
  PublicKeyState,
  Signature,
  SignatureState,
} from 'core/objects';
import { OmitMethods } from 'core/types';

export interface TransactionInputJSON {
  tx_out_hash: string;
  tx_out_index: number;
  script: TransactionInputScriptJSON;
}
export interface TransactionInputScriptJSON {
  type: string;
  pubkey: string;
  sig: string;
}

export interface TransactionInputScriptState {
  type: string;
  publicKey: PublicKeyState;
  signature: SignatureState;
}
export interface TransactionInputState {
  txOutHash: string;
  txOutIndex: number;
  script: TransactionInputScriptState;
}

export interface TransactionInputScript {
  type: string;
  publicKey: PublicKey;
  signature: Signature;
}

/**
 * An input for a coin transaction object
 */
export class TransactionInput {
  public readonly txOutHash: Hash;

  public readonly txOutIndex: number;

  public readonly script: TransactionInputScript;

  constructor(obj: OmitMethods<TransactionInput>) {
    if (obj === undefined || obj === null) {
      throw new Error(
        'Error encountered while creating a TransactionInput object: undefined/null parameters',
      );
    }

    if (obj.txOutHash === undefined) {
      throw new Error("Undefined 'txOutHash' when creating 'TransactionInput'");
    }
    this.txOutHash = obj.txOutHash;

    if (obj.txOutIndex === undefined) {
      throw new Error("Undefined 'txOutIndex' when creating 'TransactionInput'");
    }
    this.txOutIndex = obj.txOutIndex;

    if (obj.script === undefined) {
      throw new Error("Undefined 'script' when creating 'TransactionInput'");
    }
    this.script = obj.script;
  }

  public static fromState(state: TransactionInputState) {
    return new TransactionInput({
      txOutHash: new Hash(state.txOutHash),
      txOutIndex: state.txOutIndex,
      script: {
        type: state.script.type,
        publicKey: PublicKey.fromState(state.script.publicKey),
        signature: Signature.fromState(state.script.signature),
      },
    });
  }

  public toState(): TransactionInputState {
    return {
      txOutHash: this.txOutHash.toState(),
      txOutIndex: this.txOutIndex,
      script: {
        ...this.script,
        publicKey: this.script.publicKey.toState(),
        signature: this.script.signature.toState(),
      },
    };
  }

  public static fromJSON(json: TransactionInputJSON) {
    if (!json.tx_out_hash) {
      throw new ProtocolError(
        `TransactionInput.fromJSON() called on a json object without a 'tx_out_hash'`,
      );
    }

    return new TransactionInput({
      txOutHash: new Hash(json.tx_out_hash),
      txOutIndex: json.tx_out_index,
      script: {
        type: json.script.type,
        publicKey: new PublicKey(json.script.pubkey),
        signature: new Signature(json.script.sig),
      },
    });
  }

  public toJSON(): TransactionInputJSON {
    return {
      tx_out_hash: this.txOutHash.valueOf(),
      tx_out_index: this.txOutIndex,
      script: {
        type: this.script.type,
        pubkey: this.script.publicKey.valueOf(),
        sig: this.script.signature.valueOf(),
      },
    };
  }
}
