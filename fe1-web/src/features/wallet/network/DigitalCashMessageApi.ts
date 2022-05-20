import { publish } from 'core/network';
import { channelFromIds, KeyPair, PopToken, PublicKey } from 'core/objects';
import { Lao } from 'features/lao/objects';
import { OpenedLaoStore } from 'features/lao/store';

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
 * @param laoId the lao id of the roll call
 * @param rcId the id of the roll call in which to send the transaction
 */
export function requestSendTransaction(
  from: PopToken,
  to: PublicKey,
  amount: number,
  laoId: string,
  rcId: string,
): Promise<void> {
  // TODO: Should check total value, OVERFLOW

  const transactionStates = DigitalCashStore.getTransactionsByPublicKey(laoId, rcId, from.publicKey.valueOf());

  if (!transactionStates) {
    console.warn(makeErr('no transaction out were found for this public key'));
    return Promise.resolve();
  }

  const balance = getBalance(laoId, rcId, from.publicKey.valueOf());

  if (amount < 0 || amount > balance) {
    console.warn(makeErr('balance is not sufficient to send this amount'));
    return Promise.resolve();
  }

  const transaction: Transaction = Transaction.create(from, to, balance, amount, transactionStates);

  const postTransactionMessage = new PostTransaction({
    transaction_id: transaction.transactionId,
    transaction: transaction.toJSON(),
  });
  const lao: Lao = OpenedLaoStore.get();

  console.log(`Sending a transaction with id: ${transaction.transactionId.valueOf()}`);

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
  const transaction = Transaction.createCoinbase(organizerKP, to, amount);

  const postTransactionMessage = new PostTransaction({
    transaction_id: transaction.transactionId,
    transaction: transaction.toJSON(),
  });

  const lao: Lao = OpenedLaoStore.get();

  console.log(`Sending a coinbase transaction with id: ${transaction.transactionId.valueOf()}`);

  return publish(channelFromIds(lao.id), postTransactionMessage);
}
