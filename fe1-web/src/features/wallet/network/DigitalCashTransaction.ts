import { Hash, PublicKey } from 'core/objects';

export interface DigitalCashTransactionState {
  version: number;
  txsIn: TxIn[];
  txsOut: TxOut[];
  lockTime: number;
}
export interface TxInScript {
  type: string;
  pubKey: PublicKey;
  sig: string;
}
export interface TxIn {
  txOutHash: Hash;
  txOutIndex: number;
  script: TxInScript;
}
export interface TxOutScript {
  type: string;
  pubKeyHash: Hash;
}
export interface TxOut {
  value: number;
  script: TxOutScript;
}

export class DigitalCashTransaction {
  public readonly version: number;

  public readonly txsIn: TxIn[];

  public readonly txsOut: TxOut[];

  public readonly lockTime: number;

  constructor(obj: Partial<DigitalCashTransaction>) {
    if (obj === undefined || obj === null) {
      throw new Error(
        'Error encountered while creating a DigitalCashTransaction object: undefined/null parameters',
      );
    }
    if (obj.version === undefined) {
      throw new Error("Undefined 'version' when creating 'DigitalCashTransaction'");
    }
    if (obj.txsIn === undefined) {
      throw new Error("Undefined 'txsIn' when creating 'DigitalCashTransaction'");
    }
    if (obj.txsOut === undefined) {
      throw new Error("Undefined 'txsOut' when creating 'DigitalCashTransaction'");
    }
    if (obj.lockTime === undefined) {
      throw new Error("Undefined 'lockTime' when creating 'DigitalCashTransaction'");
    }

    this.version = obj.version;
    this.txsIn = obj.txsIn;
    this.txsOut = obj.txsOut;
    this.lockTime = obj.lockTime;
  }

  public static fromState(
    digitalCashTransactionState: DigitalCashTransactionState,
  ): DigitalCashTransaction {
    return new DigitalCashTransaction(digitalCashTransactionState);
  }

  public toState(): DigitalCashTransactionState {
    return {
      ...this,
    };
  }
}
