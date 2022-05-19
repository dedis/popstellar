import { Hash } from 'core/objects';
import { getStore } from 'core/redux';

import { getDigitalCashState } from '../reducer';

export namespace DigitalCashStore {
  export function getBalance(laoId: string, RCId: string, pk: string): number {
    const { balances } = getDigitalCashState(getStore().getState()).byLaoId[laoId].byRCId[RCId];
    const hash = Hash.fromString(pk);
    const balance = balances[hash.valueOf()];
    return balance || 0;
  }
}
