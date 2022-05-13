import { Hash } from 'core/objects';
import { getStore } from 'core/redux';

import { TransactionState } from '../objects/transaction';
import { getDigitalCashState } from '../reducer';

export namespace DigitalCashStore {
  export function getTransactionsByPublicKey(pk: string): TransactionState[] {
    const transactionMessagesByPKH = getDigitalCashState(
      getStore().getState(),
    ).transactionsByPubHash;
    const hash = Hash.fromString(pk);
    return Array.from(transactionMessagesByPKH[hash.valueOf()].values());
  }
}
