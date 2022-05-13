import { Hash } from 'core/objects';
import { getStore } from 'core/redux';

import { getDigitalCashState } from '../reducer/DigitalCashReducer';
import { TransactionState } from '../objects/transaction/Transaction';

export namespace DigitalCashStore {
  export function getTransactionsByPublicKey(pk: string): TransactionState[] {
    const transactionMessagesByPKH = getDigitalCashState(
      getStore().getState(),
    ).transactionsByPubHash;
    const hash = Hash.fromString(pk);
    return Array.from(transactionMessagesByPKH[hash.valueOf()].values());
  }
}
