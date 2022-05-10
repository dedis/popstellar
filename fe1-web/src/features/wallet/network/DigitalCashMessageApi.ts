import { publish } from 'core/network';
import { Base64UrlData, channelFromIds, Hash, PopToken, PublicKey } from 'core/objects';
import { Lao } from 'features/lao/objects';
import { OpenedLaoStore } from 'features/lao/store';
import STRINGS from 'resources/strings';

import { DigitalCashStore } from '../store/DigitalCashStore';
import {
  concatenateTxData,
  getTotalValue,
  getTxsInToSign,
  hashTransaction,
} from './DigitalCashHelper';
import { DigitalCashTransaction, TxIn, TxOut } from './DigitalCashTransaction';
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

  const txOutTo = {
    Value: amount,
    Script: {
      Type: STRINGS.script_type,
      PubkeyHash: toPublicKeyHash,
    },
  };

  const txOuts: TxOut[] = [txOutTo];

  if (totalValueOut > amount) {
    // Send the rest of the value back to the owner, so that the entire balance
    // is always in only one TxOut
    const txOutFrom: TxOut = {
      Value: totalValueOut - amount,
      Script: {
        Type: STRINGS.script_type,
        PubkeyHash: fromPublicKeyHash,
      },
    };
    txOuts.push(txOutFrom);
  }

  const txIns: Omit<TxIn, 'Script'>[] = getTxsInToSign(from.publicKey.valueOf(), messages);
  // Now we need to define each objects because we need some string representation of everything to hash on

  // Concatenate the data to sign
  const dataString = concatenateTxData(txIns, txOuts);

  // Sign with the popToken
  const signature = from.privateKey.sign(Base64UrlData.encode(dataString));

  // Reconstruct the txIns with the signature
  const finalTxIns: TxIn[] = txIns.map((txIn) => {
    return {
      ...txIn,
      Script: {
        Type: STRINGS.script_type,
        Pubkey: from.publicKey,
        Sig: signature,
      },
    };
  });

  const transaction: DigitalCashTransaction = {
    Version: 1,
    TxIn: finalTxIns,
    TxOut: txOuts,
    LockTime: 0,
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
 * There is no need for a sender as any coinbase transaction that is sent by
 * an organizer on the channel is considered valid
 *
 * @param to the destination public key
 * @param amount the value of the transaction
 */
export function requestCoinbaseTransaction(to: PublicKey, amount: number): Promise<void> {
  const toPublicKeyHash = Hash.fromString(to.valueOf());

  const txOuts: TxOut[] = [
    {
      Value: amount,
      Script: {
        Type: STRINGS.script_type,
        PubkeyHash: toPublicKeyHash,
      },
    },
  ];

  const txIns: TxIn[] = [
    {
      TxOutHash: new Hash(STRINGS.coinbase_hash),
      TxOutIndex: -1,
    },
  ];

  const transaction: DigitalCashTransaction = {
    Version: 1,
    TxIn: txIns,
    TxOut: txOuts,
    LockTime: 0,
  };

  const postTransactionMessage = new PostTransaction({
    transaction_id: hashTransaction(transaction),
    transaction: transaction,
  });

  const lao: Lao = OpenedLaoStore.get();

  return publish(channelFromIds(lao.id), postTransactionMessage);
}
