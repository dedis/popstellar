import { Base64UrlData, Hash, KeyPair, PopToken, PublicKey } from 'core/objects';
import { SCRIPT_TYPE, COINBASE_HASH } from 'resources/const';

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
      throw new Error("Undefined 'version' when creating 'Transaction'");
    }
    if (obj.inputs === undefined) {
      throw new Error("Undefined 'inputs' when creating 'Transaction'");
    }
    if (obj.inputs.length === 0) {
      throw new Error("Empty 'inputs' when creating 'Transaction'");
    }
    if (obj.outputs === undefined) {
      throw new Error("Undefined 'outputs' when creating 'Transaction'");
    }
    if (obj.outputs.length === 0) {
      throw new Error("Empty 'outputs' when creating 'Transaction'");
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
  private readonly hashTransaction = (): Hash => {
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

  /**
   * Creates a transaction
   * @param from the sender
   * @param to the receiver
   * @param currentBalance the current balance of the sender
   * @param amount the amount to send to the receiver
   * @param inputTransactions the transactions that contains the outputs to use as inputs in this transaction
   */
  public static create(
    from: PopToken,
    to: PublicKey,
    currentBalance: number,
    amount: number,
    inputTransactions: TransactionState[],
  ): Transaction {
    const fromPublicKeyHash = Hash.fromPublicKey(from.publicKey);

    const toPublicKeyHash = Hash.fromPublicKey(to);

    const outputTo = {
      value: amount,
      script: {
        type: SCRIPT_TYPE,
        publicKeyHash: toPublicKeyHash.valueOf(),
      },
    };

    const outputs: TransactionOutputState[] = [outputTo];

    if (currentBalance > amount) {
      // Send the rest of the value back to the owner, so that the entire balance
      // is always in only one output
      const outputFrom: TransactionOutputState = {
        value: currentBalance - amount,
        script: {
          type: SCRIPT_TYPE,
          publicKeyHash: fromPublicKeyHash.valueOf(),
        },
      };
      outputs.push(outputFrom);
    }

    const inputs: Omit<TransactionInputState, 'script'>[] = Transaction.getInputsInToSign(
      from.publicKey.valueOf(),
      inputTransactions,
    );

    // Concatenate the data to sign
    const dataString = Transaction.concatenateTxData(inputs, outputs);

    // Sign with the popToken
    const signature = from.privateKey.sign(Base64UrlData.encode(dataString));

    // Reconstruct the txIns with the signature
    const finalInputs: TransactionInputState[] = inputs.map((input) => {
      return {
        ...input,
        script: {
          type: SCRIPT_TYPE,
          publicKey: from.publicKey.valueOf(),
          signature: signature.valueOf(),
        },
      };
    });

    return Transaction.fromState({
      version: 1,
      inputs: finalInputs,
      outputs: outputs,
      lockTime: 0,
    });
  }

  /**
   * Creates a coinbase transaction
   * @param organizerKP the organizer's key pair
   * @param to the receivers of the coinbase transaction
   * @param amount the amount to send
   */
  public static createCoinbase(organizerKP: KeyPair, to: PublicKey[], amount: number): Transaction {
    const outputs: TransactionOutputState[] = to.map((pk) => ({
      value: amount,
      script: {
        type: SCRIPT_TYPE,
        publicKeyHash: Hash.fromPublicKey(pk).valueOf(),
      },
    }));

    const input: Omit<TransactionInputState, 'script'> = {
      txOutHash: COINBASE_HASH,
      txOutIndex: 0,
    };

    // Concatenate the data to sign
    const dataString = Transaction.concatenateTxData([input], outputs);

    // Sign with the popToken
    const signature = organizerKP.privateKey.sign(Base64UrlData.encode(dataString));

    // Reconstruct the inputs with the signature of the organizer
    const finalInput: TransactionInputState = {
      ...input,
      script: {
        type: SCRIPT_TYPE,
        publicKey: organizerKP.publicKey.valueOf(),
        signature: signature.valueOf(),
      },
    };

    return Transaction.fromState({
      version: 1,
      inputs: [finalInput],
      outputs: outputs,
      lockTime: 0,
    });
  }

  /**
   * Constructs a partial Input object from transaction messages to take as input
   * @param pk the public key of the sender
   * @param transactions the transaction messages used as inputs
   */
  private static readonly getInputsInToSign = (
    pk: string,
    transactions: TransactionState[],
  ): Omit<TransactionInputState, 'script'>[] =>
    transactions.flatMap((tr) =>
      tr.outputs
        .filter(
          (output) => output.script.publicKeyHash.valueOf() === Hash.fromPublicKey(pk).valueOf(),
        )
        .map((output, index) => ({
          txOutHash: tr.transactionId!.valueOf(),
          txOutIndex: index,
        })),
    );

  /**
   * Verifies the validity of the transaction
   * by checking the transaction inputs signature
   * @param organizerPublicKey - The organizer's public key of the lao
   * @param transactionStates - A transaction id mapping of all transactions in memory
   */
  public checkTransactionValidity = (
    organizerPublicKey: PublicKey,
    transactionStates: Record<string, TransactionState>,
  ): boolean => {
    // Transaction is a coinbase transaction if it's first input's txOutHash is the defined coinbase hash
    const isCoinbase = this.inputs[0].txOutHash.valueOf() === COINBASE_HASH;

    // Reconstruct data signed on
    const encodedData = Base64UrlData.encode(
      Transaction.concatenateTxData(
        this.inputs.map((input) => input.toState()),
        this.outputs.map((output) => output.toState()),
      ),
    );

    let totalInputAmount = 0;

    const inputsAreValid = this.inputs.every((input) => {
      if (isCoinbase) {
        // If the transaction is a coinbase transaction, the signer must be the organizer
        if (input.script.publicKey.valueOf() !== organizerPublicKey.valueOf()) {
          console.warn('The coinbase transaction input signer is not the organizer');
          return false;
        }
      } else {
        const originTransactionOutput =
          transactionStates[input.txOutHash.valueOf()].outputs[input.txOutIndex];

        // The public key hash of the used transaction output must correspond
        // to the public key the transaction is using in this input
        if (
          Hash.fromPublicKey(input.script.publicKey).valueOf() !==
          originTransactionOutput.script.publicKeyHash.valueOf()
        ) {
          console.warn(
            "The transaction output public key hash does not correspond to the spender's public key hash",
          );
          return false;
        }
        totalInputAmount += originTransactionOutput.value;
      }
      // The public key of this input must have signed the concatenated data
      if (!input.script.signature.verify(input.script.publicKey, encodedData)) {
        console.warn('The signature for this input is not valid');
        return false;
      }
      return true;
    });

    if (!inputsAreValid) {
      console.warn('The transaction inputs are not valid');
      return false;
    }

    let totalOutputAmount = 0;

    this.outputs.forEach((output) => {
      totalOutputAmount += output.value;
    });

    if (!isCoinbase && totalInputAmount < totalOutputAmount) {
      console.warn('The total transaction output value is bigger than the total input value');
      return false;
    }

    return true;
  };

  /**
   * Concatenates the partial inputs and the outputs in a string to sign over it by following the digital cash specification
   * @param inputs
   * @param outputs
   */
  public static concatenateTxData = (
    inputs: Omit<TransactionInputState, 'script'>[],
    outputs: TransactionOutputState[],
  ) => {
    const inputsDataString = inputs.reduce(
      (dataString, input) => dataString + input.txOutHash.valueOf() + input.txOutIndex.toString(),
      '',
    );
    return outputs.reduce(
      (dataString, output) =>
        dataString +
        output.value.toString() +
        output.script.type +
        output.script.publicKeyHash.valueOf(),
      inputsDataString,
    );
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
      version: this.version,
      inputs: this.inputs.map((input) => input.toState()),
      outputs: this.outputs.map((output) => output.toState()),
      transactionId: this.transactionId.valueOf(),
      lockTime: this.lockTime,
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
