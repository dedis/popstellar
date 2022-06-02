import { Hash } from 'core/objects';
import { getStore } from 'core/redux';

import { TransactionState } from '../objects/transaction';
import { getDigitalCashState } from '../reducer';

export namespace DigitalCashStore {
  export function getTransactionsByPublicKey(
    laoId: string,
    RCId: string,
    pk: string,
  ): TransactionState[] {
    const laoState = getDigitalCashState(getStore().getState()).byLaoId[laoId];
    if (!laoState) {
      console.warn('This lao is undefined in digital cash state');
      return [];
    }
    const rcState = laoState.byRCId[RCId];
    if (!rcState) {
      console.warn('This roll call is undefined in digital cash state');
      return [];
    }
    const hash = Hash.fromPublicKey(pk);
    return Array.from(rcState.transactionsByPubHash[hash.valueOf()]?.values() || []) || [];
  }
  export function getBalance(laoId: string, RCId: string, pk: string): number {
    const laoState = getDigitalCashState(getStore().getState()).byLaoId[laoId];
    if (!laoState) {
      console.warn('This lao is undefined in digital cash state');
      return 0;
    }
    const rcState = laoState.byRCId[RCId];
    if (!rcState) {
      console.warn('This roll call is undefined in digital cash state');
      return 0;
    }
    const hash = Hash.fromPublicKey(pk);
    const balance = rcState.balances[hash.valueOf()];
    return balance || 0;
  }
}
