import { publish } from 'core/network';
import { Base64UrlData, channelFromIds, Hash, KeyPair, PopToken, PublicKey } from 'core/objects';
import { Lao } from 'features/lao/objects';
import { OpenedLaoStore } from 'features/lao/store';
import STRINGS from 'resources/strings';

import { DigitalCashStore } from '../store/DigitalCashStore';
import {
  concatenateTxData,
  getTotalValue,
  getInputsInToSign,
  hashTransaction,
} from './DigitalCashHelper';
import { DigitalCashTransaction, Input, Output } from './DigitalCashTransaction';
import { PostTransaction } from './messages/PostTransaction';

const makeErr = (err: string) => `Sending the transaction failed: ${err}`;

/**
 * Requests a digital cash transaction post
 * @param from the popToken to send it with
 * @param to the destination public key
 * @param amount the value of the transaction
 */
export function requestSendTransaction(
  from: PopToken,
  to: PublicKey,
  amount: number,
): Promise<void> {
  // TODO: Should check total value, OVERFLOW

  const fromPublicKeyHash = Hash.fromString(from.publicKey.valueOf());
  const toPublicKeyHash = Hash.fromString(to.valueOf());

  const messages = DigitalCashStore.getTransactionsByPublicKey(from.publicKey.valueOf());

  if (!messages) {
    console.warn(makeErr('no transaction out were found for this public key'));
    return Promise.resolve();
  }

  const totalValueOut = getTotalValue(fromPublicKeyHash, messages);

  if (amount < 0 || amount > totalValueOut) {
    console.warn(makeErr('balance is not sufficient to send this amount'));
    return Promise.resolve();
  }

  const outputTo = {
    value: amount,
    script: {
      type: STRINGS.script_type,
      publicKeyHash: toPublicKeyHash,
    },
  };

  const outputs: Output[] = [outputTo];

  if (totalValueOut > amount) {
    // Send the rest of the value back to the owner, so that the entire balance
    // is always in only one TxOut
    const outputFrom: Output = {
      value: totalValueOut - amount,
      script: {
        type: STRINGS.script_type,
        publicKeyHash: fromPublicKeyHash,
      },
    };
    outputs.push(outputFrom);
  }

  const inputs: Omit<Input, 'script'>[] = getInputsInToSign(from.publicKey.valueOf(), messages);
  // Now we need to define each objects because we need some string representation of everything to hash on

  // Concatenate the data to sign
  const dataString = concatenateTxData(outputs, inputs);

  // Sign with the popToken
  const signature = from.privateKey.sign(Base64UrlData.encode(dataString));

  // Reconstruct the txIns with the signature
  const finalInputs: Input[] = inputs.map((input) => {
    return {
      ...input,
      script: {
        type: STRINGS.script_type,
        publicKey: from.publicKey,
        signature: signature,
      },
    };
  });

  const transaction: DigitalCashTransaction = {
    version: 1,
    inputs: finalInputs,
    outputs: outputs,
    lockTime: 0,
  };

  const postTransactionMessage = new PostTransaction({
    transaction_id: hashTransaction(transaction),
    transaction: transaction,
  });
  const lao: Lao = OpenedLaoStore.get();

  return publish(channelFromIds(lao.id), postTransactionMessage);
}

/**
 * Requests a digital cash coinbase transaction post
 *
 * @param organizerKP the keypair of the organizer
 * @param to the destination public key
 * @param amount the value of the transaction
 */
export function requestCoinbaseTransaction(
  organizerKP: KeyPair,
  to: PublicKey,
  amount: number,
): Promise<void> {
  const toPublicKeyHash = Hash.fromString(to.valueOf());

  const outputTo = {
    value: amount,
    script: {
      type: STRINGS.script_type,
      publicKeyHash: toPublicKeyHash,
    },
  };

  const outputs: Output[] = [outputTo];

  // Concatenate the data to sign
  const dataString = concatenateTxData(outputs);

  // Sign with the popToken
  const signature = organizerKP.privateKey.sign(Base64UrlData.encode(dataString));

  // Reconstruct the inputs with the signature of the organizer
  const inputs: Input[] = [
    {
      script: {
        type: STRINGS.script_type,
        publicKey: organizerKP.publicKey,
        signature: signature,
      },
    },
  ];

  const transaction: DigitalCashTransaction = {
    version: 1,
    inputs: inputs,
    outputs: outputs,
    lockTime: 0,
  };

  const postTransactionMessage = new PostTransaction({
    transaction_id: hashTransaction(transaction),
    transaction: transaction,
  });

  const lao: Lao = OpenedLaoStore.get();

  return publish(channelFromIds(lao.id), postTransactionMessage);
}
