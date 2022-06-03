import { Hash } from 'core/objects';
import { getStore } from 'core/redux';

import { TransactionState } from '../objects/transaction';
import { DigitalCashReducerState, getDigitalCashState } from '../reducer';

export namespace DigitalCashStore {
  export function getRollCallState(
    laoId: string,
    rollCallId: string,
  ): DigitalCashReducerState | undefined {
    const laoState = getDigitalCashState(getStore().getState()).byLaoId[laoId];
    if (!laoState) {
      console.warn('This lao is undefined in digital cash state');
      return undefined;
    }
    const rcState = laoState.byRCId[rollCallId];
    if (!rcState) {
      console.warn('This roll call is undefined in digital cash state');
      return undefined;
    }
    return rcState;
  }
  export function getTransactionsByPublicKey(
    laoId: string,
    rollCallId: string,
    pk: string,
  ): TransactionState[] {
    const rcState = getRollCallState(laoId, rollCallId);
    if (!rcState) {
      return [];
    }
    const hash = Hash.fromPublicKey(pk);
    return Array.from(rcState.transactionsByPubHash[hash.valueOf()]?.values() || []) || [];
  }
  export function getBalance(laoId: string, rollCallId: string, pk: string): number {
    const rcState = getRollCallState(laoId, rollCallId);
    if (!rcState) {
      return 0;
    }
    const hash = Hash.fromPublicKey(pk);
    const balance = rcState.balances[hash.valueOf()];
    return balance || 0;
  }
}
