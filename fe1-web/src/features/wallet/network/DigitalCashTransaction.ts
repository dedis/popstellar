import { Hash, PublicKey, Signature } from 'core/objects';

export interface DigitalCashTransactionState {
  version: number;
  inputs: InputState[];
  outputs: OutputState[];
  lockTime: number;
}
export interface DigitalCashMessageState {
  transaction: DigitalCashTransactionState;
  transactionId: string;
}
export interface OutputState {
  value: number;
  script: OutputScriptState;
}
export interface InputScriptState {
  type: string;
  publicKey: string;
  signature: string;
}
export interface InputState {
  txOutHash: string | undefined;
  txOutIndex: number | undefined;
  script: InputScriptState;
}
export interface OutputScriptState {
  type: string;
  publicKeyHash: string;
}

export interface DigitalCashTransaction {
  version: number;
  inputs: Input[];
  outputs: Output[];
  lockTime: number;
}
export interface InputScript {
  type: string;
  publicKey: PublicKey;
  signature: Signature;
}
export interface Input {
  txOutHash?: Hash | undefined;
  txOutIndex?: number | undefined;
  script: InputScript;
}
export interface OutputScript {
  type: string;
  publicKeyHash: Hash;
}
export interface Output {
  value: number;
  script: OutputScript;
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
        outputs: digitalCashMessageState.transaction.outputs.map((outputState) => {
          return {
            ...outputState,
            script: {
              type: outputState.script.type,
              publicKeyHash: new Hash(outputState.script.publicKeyHash),
            },
          };
        }),
        inputs: digitalCashMessageState.transaction.inputs.map((inputState) => {
          return {
            txOutIndex: inputState.txOutIndex,
            txOutHash: inputState.txOutHash ? new Hash(inputState.txOutHash) : undefined,
            script: {
              type: inputState.script.type,
              publicKey: new PublicKey(inputState.script.publicKey),
              signature: new Signature(inputState.script.signature),
            },
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
        outputs: this.transaction.outputs.map((output) => {
          return {
            ...output,
            script: {
              type: output.script.type,
              publicKeyHash: output.script.publicKeyHash.valueOf(),
            },
          };
        }),
        inputs: this.transaction.inputs.map((input) => {
          return {
            txOutIndex: input.txOutIndex,
            txOutHash: input.txOutHash?.valueOf(),
            script: {
              type: input.script.type,
              publicKey: input.script.publicKey.valueOf(),
              signature: input.script.signature.valueOf(),
            },
          };
        }),
      },
      transactionId: this.transactionId.valueOf(),
    };
  }
}
