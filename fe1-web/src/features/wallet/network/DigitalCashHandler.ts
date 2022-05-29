import { ActionType, ObjectType, ProcessableMessage } from 'core/network/jsonrpc/messages';
import { Hash } from 'core/objects';

import { Transaction } from '../objects/transaction';
import { PostTransaction } from './messages';

/**
 * Handle a PostTransaction message.
 *
 * @param addTransaction the function to add the received transaction to the digital cash state
 */
export const handleTransactionPost =
  (addTransaction: (laoId: Hash, rcId: Hash, transaction: Transaction) => void) =>
  (msg: ProcessableMessage): boolean => {
    if (
      msg.messageData.object !== ObjectType.COIN ||
      msg.messageData.action !== ActionType.POST_TRANSACTION
    ) {
      console.warn('handleTransactionPost was called to process an unsupported message', msg);
      return false;
    }

    if (!msg.laoId) {
      console.warn(
        `The PostTransaction message not sent on an lao subchannel but rather on '${msg.channel}'`,
      );
      return false;
    }

    const tx = msg.messageData as PostTransaction;
    console.log(`Handler: Received transaction with id: ${tx.transaction_id.valueOf()}`);

    const transaction = Transaction.fromJSON(tx.transaction, tx.transaction_id.valueOf());
    addTransaction(msg.laoId, tx.rc_id, transaction);
    return true;
  };
