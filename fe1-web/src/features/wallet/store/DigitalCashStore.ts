import { Hash } from 'core/objects';
import { getStore } from 'core/redux';

import { TransactionState } from '../objects/transaction';
import { getDigitalCashState } from '../reducer';

export namespace DigitalCashStore {
  export function getTransactionsByPublicKey(
    laoId: string,
    pk: string,
  ): TransactionState[] {
    const laoState = getDigitalCashState(getStore().getState()).byLaoId[laoId];
    if (!laoState) {
      return [];
    }
    const hash = Hash.fromPublicKey(pk);
    return Array.from(laoState.transactionsByPubHash[hash.valueOf()]?.values() || []) || [];
  }
  export function getBalance(laoId: string, pk: string): number {
    const laoState = getDigitalCashState(getStore().getState()).byLaoId[laoId];
    if (!laoState) {
      return 0;
    }
    const hash = Hash.fromPublicKey(pk);
    const balance = laoState.balances[hash.valueOf()];
    return balance || 0;
  }
}
