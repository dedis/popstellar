import { Hash } from 'core/objects';

import { TransactionState } from './Transaction';
import { TransactionInputState } from './TransactionInput';
import { TransactionOutputState } from './TransactionOutput';

/**
 * Hash a transaction to get its id
 * @param transaction to hash
 */
export const hashTransaction = (transaction: TransactionState): Hash => {
  // Recursively concatenating fields by lexicographic order of their names
  const dataInputs = transaction.inputs.flatMap((input) => {
    if (input.txOutHash && input.txOutIndex) {
      // Might be a coinbase transaction
      return [
        input.script.publicKey,
        input.script.signature,
        input.script.type,
        input.txOutHash,
        input.txOutIndex.toString(),
      ];
    }
    return [input.script.publicKey.valueOf(), input.script.signature.valueOf(), input.script.type];
  });
  const dataOutputs = transaction.outputs.flatMap((output) => {
    return [output.script.publicKeyHash.valueOf(), output.script.type, output.value.toString()];
  });
  const data = [transaction.lockTime.toString()]
    .concat(dataInputs)
    .concat(dataOutputs)
    .concat([transaction.version.toString()]);

  // Hash will take care of concatenating each fields length
  return Hash.fromStringArray(...data);
};

/**
 * Get the total value out that corresponds to this public key hash from an array of transactions
 * @param pkHash the public key hash
 * @param transactionMessages the transaction messages from which the amount out
 * @return the total value out
 */
export const getTotalValue = (pkHash: string | Hash, transactions: TransactionState[]): number => {
  const outputs = transactions.flatMap((tr) =>
    tr.outputs.filter((output) => output.script.publicKeyHash.valueOf() === pkHash.valueOf()),
  );
  return outputs.reduce((total, current) => total + current.value, 0);
};

/**
 * Constructs a partial Input object from transaction messages to take as input
 * @param pk the public key of the sender
 * @param transactionMessages the transaction messages used as inputs
 */
export const getInputsInToSign = (
  pk: string,
  transactions: TransactionState[],
): Omit<TransactionInputState, 'script'>[] => {
  return transactions.flatMap((tr) =>
    tr.outputs
      .filter((output) => output.script.publicKeyHash.valueOf() === Hash.fromString(pk).valueOf())
      .map((output, index) => {
        return {
          txOutHash: hashTransaction(tr).valueOf(),
          txOutIndex: index,
        };
      }),
  );
};

/**
 * Concatenates the partial inputs and the outputs in a string to sign over it by following the digital cash specification
 * @param inputs left empty if this is a coinbase transaction
 * @param outputs
 */
export const concatenateTxData = (
  outputs: TransactionOutputState[],
  inputs: Omit<TransactionInputState, 'script'>[] = [],
) => {
  let inputsDataString = '';
  if (inputs.length > 0) {
    inputsDataString = inputs.reduce(
      (dataString, input) => dataString + input.txOutHash!.valueOf() + input.txOutIndex!.toString(),
      '',
    );
  }
  return outputs.reduce(
    (dataString, output) =>
      dataString +
      output.value.toString() +
      output.script.type +
      output.script.publicKeyHash.valueOf(),
    inputsDataString,
  );
};
