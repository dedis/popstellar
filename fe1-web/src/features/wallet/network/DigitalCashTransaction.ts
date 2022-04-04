import { Hash, PublicKey } from 'core/objects';

export interface TxInScript {
  type: string;
  pubKey: PublicKey;
  sig: string; // Hash ?
}
export interface TxIn {
  txOutHash: Hash;
  txOutIndex: number;
  script: TxInScript;
}
export interface TxOutScript {
  type: string;
  pubKeyHash: string; // Hash ?
}
export interface TxOut {
  value: number; // BigInteger ?
  script: TxOutScript;
}

export class DigitalCashTransaction {
  public readonly version: number;

  public readonly txsIn: TxIn[];

  public readonly txsOut: TxOut[];

  public readonly lockTime: number;
}
