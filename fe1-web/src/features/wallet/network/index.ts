import { ActionType, MessageRegistry, ObjectType } from 'core/network/jsonrpc/messages';
import { Hash, PublicKey } from 'core/objects';
import { dispatch } from 'core/redux';

import { Transaction } from '../objects/transaction';
import { addTransaction } from '../reducer';
import { handleTransactionPost } from './DigitalCashHandler';
import { PostTransaction } from './messages';

export * from './DigitalCashMessageApi';

/**
 * Configures the network callbacks in a MessageRegistry.
 *
 * @param registry - The MessageRegistry where we want to add the mappings
 * @param getLaoOrganizer - A function to get the organizer from a lao id
 */
export function configureNetwork(
  registry: MessageRegistry,
  getLaoOrganizer: (laoId: string) => PublicKey | undefined,
) {
  const addTransactionToState = (laoId: Hash, transaction: Transaction) => {
    dispatch(
      addTransaction({
        laoId: laoId.valueOf(),
        transactionMessage: transaction.toState(),
      }),
    );
  };

  registry.add(
    ObjectType.COIN,
    ActionType.POST_TRANSACTION,
    handleTransactionPost(addTransactionToState, getLaoOrganizer),
    PostTransaction.fromJSON,
  );
}
