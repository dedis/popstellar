import { Hash, PopToken, PublicKey } from 'core/objects';
import STRINGS from 'resources/strings';

import { DigitalCashStore } from '../store/DigitalCashStore';
import { getPartialTxsIn, getTotalValue } from './DigitalCashHelper';
import { TxIn, TxOut } from './DigitalCashTransaction';

export function requestSendTransaction(from: PopToken, to: PublicKey, amount: number) {
  // 1. Find all transactions with the "from" public key (hash) in their txOut
  // 2. Compute the total value of all these txOuts and check that it is bigger than value
  // 3. Create a new transaction with value sent to "to" and the rest of the balance to "from"

  const makeErr = (err: string) => `Sending the transaction failed: ${err}`;

  const fromPublicKeyHash = Hash.fromString(from.publicKey.valueOf());
  const toPublicKeyHash = Hash.fromString(to.valueOf());

  const messages = DigitalCashStore.getTransactionsByPublicKey(from.publicKey.valueOf());

  if (!messages) {
    console.warn(makeErr('no transaction out were found for this public key'));
    return;
  }

  const totalValueOut = getTotalValue(fromPublicKeyHash, messages);

  if (amount < 0 || amount > totalValueOut) {
    console.warn(makeErr('balance is not sufficient to send this amount'));
    return;
  }

  const txOutTo = {
    value: amount,
    script: {
      type: STRINGS.script_type,
      publicKeyHash: toPublicKeyHash,
    },
  };

  const txOuts: TxOut[] = [txOutTo];

  if (totalValueOut > amount) {
    // Send the rest of the value back to the owner, so that the entire balance
    // is always in only one TxOut
    const txOutFrom: TxOut = {
      value: totalValueOut - amount,
      script: {
        type: STRINGS.script_type,
        publicKeyHash: fromPublicKeyHash,
      },
    };
    txOuts.push(txOutFrom);
  }

  const partialTxIns: Partial<TxIn>[] = getPartialTxsIn(from.publicKey.valueOf(), messages);

  // Now we need to define each objects because we need some string representation of everything to hash on
}
