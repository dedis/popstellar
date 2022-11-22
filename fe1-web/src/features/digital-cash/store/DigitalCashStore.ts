import { Hash, PublicKey } from 'core/objects';
import { getStore } from 'core/redux';

import { TransactionState } from '../objects/transaction';
import { getDigitalCashState } from '../reducer';

/**
 * Digital cash namespace to access the digital cash store
 *
 * Those functions should not be called inside a component
 */
export namespace DigitalCashStore {
  /**
   * Get all transactions from a lao
   */
  export function getTransactionsById(laoId: Hash): Record<string, TransactionState> {
    const serializedLaoId = laoId.valueOf();

    return (
      getDigitalCashState(getStore().getState()).byLaoId[serializedLaoId]?.transactionsByHash || {}
    );
  }

  /**
   * Gets all transactions available for this public key in this lao
   */
  export function getTransactionsByPublicKey(laoId: Hash, pk: PublicKey): TransactionState[] {
    const serializedLaoId = laoId.valueOf();

    const laoState = getDigitalCashState(getStore().getState()).byLaoId[serializedLaoId];
    if (!laoState) {
      return [];
    }
    const pkHash = Hash.fromPublicKey(pk);
    const transactionsById = getTransactionsById(laoId);
    return laoState.transactionsByPubHash[pkHash.valueOf()]?.map((hash) => transactionsById[hash]);
  }

  /**
   * Gets the balance of a particular public key in a lao
   */
  export function getBalance(laoId: Hash, pk: PublicKey): number {
    const serializedLaoId = laoId.valueOf();

    const laoState = getDigitalCashState(getStore().getState()).byLaoId[serializedLaoId];
    if (!laoState) {
      return 0;
    }
    const hash = Hash.fromPublicKey(pk);
    const balance = laoState.balances[hash.valueOf()];
    return balance || 0;
  }
}
