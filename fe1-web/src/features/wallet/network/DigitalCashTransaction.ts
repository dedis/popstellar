import { Hash, PublicKey, Signature } from 'core/objects';

export interface DigitalCashTransactionState {
  Version: number;
  TxIn: TxInState[];
  TxOut: TxOutState[];
  LockTime: number;
}
export interface DigitalCashMessageState {
  transaction: DigitalCashTransactionState;
  transactionId: string;
}
export interface TxOutState {
  Value: number;
  Script: TxOutScriptState;
}
export interface TxInScriptState {
  Type: string;
  Pubkey: string;
  Sig: string;
}

export interface TxInState {
  TxOutHash: string;
  TxOutIndex: number;
  Script: TxInScriptState;
}
export interface TxOutScriptState {
  Type: string;
  PubkeyHash: string;
}

export interface DigitalCashTransaction {
  Version: number;
  TxIn: TxIn[];
  TxOut: TxOut[];
  LockTime: number;
}
export interface TxInScript {
  Type: string;
  Pubkey: PublicKey;
  Sig: Signature;
}
export interface TxIn {
  TxOutHash: Hash;
  TxOutIndex: number;
  Script: TxInScript | any; // TODO: Script might be empty if coinbase transaction, how to handle it ? ??????
}
export interface TxOutScript {
  Type: string;
  PubkeyHash: Hash;
}
export interface TxOut {
  Value: number;
  Script: TxOutScript;
}
export class DigitalCashMessage {
  public readonly transactionId: Hash;

  public readonly transaction: DigitalCashTransaction;

  constructor(obj: Partial<DigitalCashMessage>) {
    if (obj === undefined || obj === null) {
      throw new Error(
        'Error encountered while creating a DigitalCashMessage object: undefined/null parameters',
      );
    }
    if (obj.transaction === undefined) {
      throw new Error("Undefined 'transaction' when creating 'DigitalCashMessage'");
    }
    if (obj.transactionId === undefined) {
      throw new Error("Undefined 'transactionID' when creating 'DigitalCashMessage'");
    }

    this.transaction = obj.transaction;
    this.transactionId = obj.transactionId;
  }

  public static fromState(digitalCashMessageState: DigitalCashMessageState): DigitalCashMessage {
    return new DigitalCashMessage({
      transaction: {
        Version: digitalCashMessageState.transaction.Version,
        TxOut: digitalCashMessageState.transaction.TxOut.map((txOutState) => {
          return {
            ...txOutState,
            Script: {
              Type: txOutState.Script.Type,
              PubkeyHash: new Hash(txOutState.Script.PubkeyHash),
            },
          };
        }),
        TxIn: digitalCashMessageState.transaction.TxIn.map((txInState) => {
          return {
            ...txInState,
            TxOutHash: new Hash(txInState.TxOutHash),
            Script: txInState.Script.Type // In the case of a coinbase transaction
              ? {
                  Type: txInState.Script.Type,
                  Pubkey: new PublicKey(txInState.Script.Pubkey),
                  Sig: new Signature(txInState.Script.Sig),
                }
              : {},
          };
        }),
        LockTime: digitalCashMessageState.transaction.LockTime,
      },
      transactionId: new Hash(digitalCashMessageState.transactionId),
    });
  }

  public toState(): DigitalCashMessageState {
    return {
      transaction: {
        ...this.transaction,
        TxOut: this.transaction.TxOut.map((txOut) => {
          return {
            ...txOut,
            Script: {
              Type: txOut.Script.Type,
              PubkeyHash: txOut.Script.PubkeyHash.valueOf(),
            },
          };
        }),
        TxIn: this.transaction.TxIn.map((txIn) => {
          return {
            ...txIn,
            TxOutHash: txIn.TxOutHash.valueOf(),
            Script: {
              Type: txIn.Script.Type,
              Pubkey: txIn.Script.Pubkey.valueOf(),
              Sig: txIn.Script.Sig.valueOf(),
            },
          };
        }),
      },
      transactionId: this.transactionId.valueOf(),
    };
  }
}
