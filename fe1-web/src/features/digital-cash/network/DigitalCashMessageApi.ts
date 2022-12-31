import { publish } from 'core/network';
import { getCoinChannel, Hash, KeyPair, PopToken, PublicKey } from 'core/objects';

import { Transaction } from '../objects/transaction';
import { DigitalCashStore } from '../store';
import { PostTransaction } from './messages';

import getBalance = DigitalCashStore.getBalance;

const makeErr = (err: string) => `Sending the transaction failed: ${err}`;

/**
 * Requests a digital cash transaction post
 * @param from the popToken to send it with
 * @param to the destination public key
 * @param amount the value of the transaction
 * @param laoId the id of the lao in which to send the transaction
 */
export async function requestSendTransaction(
  from: PopToken,
  to: PublicKey,
  amount: number,
  laoId: Hash,
): Promise<void> {
  const transactionStates = DigitalCashStore.getTransactionsByPublicKey(laoId, from.publicKey);

  if (transactionStates.length === 0) {
    throw new Error(makeErr('no transaction out were found for this public key'));
  }

  const balance = getBalance(laoId, from.publicKey);

  if (amount < 0 || amount > balance) {
    throw new Error(makeErr('balance is not sufficient to send this amount'));
  }

  const transaction: Transaction = Transaction.create(from, to, balance, amount, transactionStates);

  const postTransactionMessage = new PostTransaction({
    transaction_id: transaction.transactionId,
    transaction: transaction.toJSON(),
  });

  console.log(`Sending a transaction with id: ${transaction.transactionId.valueOf()}`);

  return publish(getCoinChannel(laoId), postTransactionMessage);
}

/**
 * Requests a digital cash coinbase transaction post
 *
 * @param organizerKP the keypair of the organizer
 * @param to the destinations' public key
 * @param amount the value of the transaction
 * @param laoId the lao id in which to send the transaction
 */
export function requestCoinbaseTransaction(
  organizerKP: KeyPair,
  to: PublicKey[],
  amount: number,
  laoId: Hash,
): Promise<void> {
  const transaction = Transaction.createCoinbase(organizerKP, to, amount);

  const postTransactionMessage = new PostTransaction({
    transaction_id: transaction.transactionId,
    transaction: transaction.toJSON(),
  });

  console.log(`Sending a coinbase transaction with id: ${transaction.transactionId.valueOf()}`);

  return publish(getCoinChannel(laoId), postTransactionMessage);
}
