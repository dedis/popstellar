import { Hash } from 'core/objects';

import { TransactionInput, TransactionInputJSON, TransactionInputState } from './TransactionInput';
import {
  TransactionOutput,
  TransactionOutputJSON,
  TransactionOutputState,
} from './TransactionOutput';

export interface TransactionJSON {
  version: number;
  inputs: TransactionInputJSON[];
  outputs: TransactionOutputJSON[];
  lock_time: number;
}

export interface TransactionState {
  version: number;
  inputs: TransactionInputState[];
  outputs: TransactionOutputState[];
  lockTime: number;
  transactionId?: string;
}

/**
 * A coin transaction object
 */
export class Transaction {
  public readonly version: number;

  public readonly inputs: TransactionInput[];

  public readonly outputs: TransactionOutput[];

  public readonly lockTime: number;

  public readonly transactionId: Hash;

  constructor(obj: Partial<Transaction>) {
    if (obj === undefined || obj === null) {
      throw new Error(
        'Error encountered while creating a Transaction object: undefined/null parameters',
      );
    }
    if (obj.version === undefined) {
      throw new Error("Undefined 'version' when creating 'DigitalCashMessage'");
    }
    if (obj.inputs === undefined) {
      throw new Error("Undefined 'inputs' when creating 'Transaction'");
    }
    if (obj.outputs === undefined) {
      throw new Error("Undefined 'outputs' when creating 'Transaction'");
    }
    if (obj.lockTime === undefined) {
      throw new Error("Undefined 'lockTime' when creating 'Transaction'");
    }

    this.version = obj.version;
    this.inputs = obj.inputs;
    this.outputs = obj.outputs;
    this.lockTime = obj.lockTime;

    if (obj.transactionId === undefined) {
      this.transactionId = this.hashTransaction();
    } else {
      if (obj.transactionId.valueOf() !== this.hashTransaction().valueOf()) {
        throw new Error(
          "The computed transaction hash does not correspond to the provided one when creating 'Transaction'",
        );
      }
      this.transactionId = obj.transactionId;
    }
  }

  /**
   * Hashes the transaction to get its id
   */
  private hashTransaction = (): Hash => {
    // Recursively concatenating fields by lexicographic order of their names
    const dataInputs = this.inputs.flatMap((input) => {
      return [
        input.script.publicKey.valueOf(),
        input.script.signature.valueOf(),
        input.script.type,
        input.txOutHash.valueOf(),
        input.txOutIndex.toString(),
      ];
    });
    const dataOutputs = this.outputs.flatMap((output) => {
      return [output.script.publicKeyHash.valueOf(), output.script.type, output.value.toString()];
    });
    const data = dataInputs
      .concat([this.lockTime.toString()])
      .concat(dataOutputs)
      .concat([this.version.toString()]);

    // Hash will take care of concatenating each fields length
    return Hash.fromStringArray(...data);
  };

  public static fromState(transactionState: TransactionState): Transaction {
    return new Transaction({
      ...transactionState,
      inputs: transactionState.inputs.map((input) => TransactionInput.fromState(input)),
      outputs: transactionState.outputs.map((output) => TransactionOutput.fromState(output)),
      transactionId: transactionState.transactionId
        ? new Hash(transactionState.transactionId)
        : undefined,
    });
  }

  public toState(): TransactionState {
    return {
      ...this,
      inputs: this.inputs.map((input) => input.toState()),
      outputs: this.outputs.map((output) => output.toState()),
      transactionId: this.transactionId.valueOf(),
    };
  }

  public static fromJSON(transactionJSON: TransactionJSON, transactionId: string) {
    return new Transaction({
      version: transactionJSON.version,
      inputs: transactionJSON.inputs.map((input) => TransactionInput.fromJSON(input)),
      outputs: transactionJSON.outputs.map((output) => TransactionOutput.fromJSON(output)),
      lockTime: transactionJSON.lock_time,
      transactionId: new Hash(transactionId),
    });
  }

  public toJSON(): TransactionJSON {
    return {
      version: this.version,
      inputs: this.inputs.map((input) => input.toJSON()),
      outputs: this.outputs.map((output) => output.toJSON()),
      lock_time: this.lockTime,
    };
  }
}
