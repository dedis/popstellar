import { Hash } from 'core/objects';
import { getStore } from 'core/redux';

import { getDigitalCashState } from '../reducer/DigitalCashReducer';

export namespace DigitalCashStore {
  export function getTransactionsByPublicKey(pk: string) {
    const transactionMessagesByPKH = getDigitalCashState(
      getStore().getState(),
    ).transactionsMessagesByPubHash;
    const hash = Hash.fromString(pk);
    return Array.from(transactionMessagesByPKH[hash.valueOf()].values());
  }
}
