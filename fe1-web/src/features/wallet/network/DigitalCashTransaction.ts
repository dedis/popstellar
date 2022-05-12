import { Hash, PublicKey, Signature } from 'core/objects';

/**
 * TODO: change the naming convention <==> to coordinate with backend
 * The naming does not follow the usual convention, but as this was not the priority,
 * we will fix it later
 */

export interface DigitalCashTransactionState {
  version: number;
  txsIn: TxInState[];
  txsOut: TxOutState[];
  lockTime: number;
}
export interface DigitalCashMessageState {
  transaction: DigitalCashTransactionState;
  transactionId: string;
}
export interface TxOutState {
  value: number;
  script: TxOutScriptState;
}
export interface TxInScriptState {
  type: string;
  publicKey: string;
  signature: string;
}

export interface TxInState {
  txOutHash: string;
  txOutIndex: number;
  script: TxInScriptState;
}
export interface TxOutScriptState {
  type: string;
  publicKeyHash: string;
}

export interface DigitalCashTransaction {
  version: number;
  txsIn: TxIn[];
  txsOut: TxOut[];
  lockTime: number;
}
export interface TxInScript {
  type: string;
  publicKey: PublicKey;
  signature: Signature;
}
export interface TxIn {
  txOutHash: Hash;
  txOutIndex: number;
  script: TxInScript | any; // TODO: Script might be empty if coinbase transaction, how to handle it ?
}
export interface TxOutScript {
  type: string;
  publicKeyHash: Hash;
}
export interface TxOut {
  value: number;
  script: TxOutScript;
}

/**
 * A digital cash message object
 */
export class DigitalCashMessage {
  public readonly transactionId: Hash;

  public readonly transaction: DigitalCashTransaction;

  constructor(obj: Partial<DigitalCashMessage>) {
    if (obj === undefined || obj === null) {
      throw new Error(
        'Error encountered while creating a DigitalCashTransaction object: undefined/null parameters',
      );
    }
    if (obj.transaction === undefined) {
      throw new Error("Undefined 'transaction' when creating 'DigitalCashMessage'");
    }
    if (obj.transactionId === undefined) {
      throw new Error("Undefined 'transactionID' when creating 'DigitalCashTransaction'");
    }

    this.transaction = obj.transaction;
    this.transactionId = obj.transactionId;
  }

  public static fromState(digitalCashMessageState: DigitalCashMessageState): DigitalCashMessage {
    return new DigitalCashMessage({
      transaction: {
        version: digitalCashMessageState.transaction.version,
        txsOut: digitalCashMessageState.transaction.txsOut.map((txOutState) => {
          return {
            ...txOutState,
            script: {
              type: txOutState.script.type,
              publicKeyHash: new Hash(txOutState.script.publicKeyHash),
            },
          };
        }),
        txsIn: digitalCashMessageState.transaction.txsIn.map((txInState) => {
          return {
            ...txInState,
            txOutHash: new Hash(txInState.txOutHash),
            script: txInState.script.type // In the case of a coinbase transaction
              ? {
                  type: txInState.script.type,
                  publicKey: new PublicKey(txInState.script.publicKey),
                  signature: new Signature(txInState.script.signature),
                }
              : {},
          };
        }),
        lockTime: digitalCashMessageState.transaction.lockTime,
      },
      transactionId: new Hash(digitalCashMessageState.transactionId),
    });
  }

  public toState(): DigitalCashMessageState {
    return {
      transaction: {
        ...this.transaction,
        txsOut: this.transaction.txsOut.map((txOut) => {
          return {
            ...txOut,
            script: {
              type: txOut.script.type,
              publicKeyHash: txOut.script.publicKeyHash.valueOf(),
            },
          };
        }),
        txsIn: this.transaction.txsIn.map((txIn) => {
          return {
            ...txIn,
            txOutHash: txIn.txOutHash.valueOf(),
            script: {
              type: txIn.script.type,
              publicKey: txIn.script.publicKey.valueOf(),
              signature: txIn.script.signature.valueOf(),
            },
          };
        }),
      },
      transactionId: this.transactionId.valueOf(),
    };
  }
}
