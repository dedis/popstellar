import { ActionType, ObjectType, ProcessableMessage } from 'core/network/jsonrpc/messages';

import { PostTransaction } from './messages/PostTransaction';

/**
 * Handle a RollCallOpen message by opening the corresponding roll call.
 *
 * @param msg - The extended message for opening a roll call
 */
export function handleTransactionPost(msg: ProcessableMessage): boolean {
  if (
    msg.messageData.object !== ObjectType.TRANSACTION ||
    msg.messageData.action !== ActionType.POST
  ) {
    console.warn('handleTransactionPost was called to process an unsupported message', msg);
    return false;
  }
  const tx = msg.messageData as PostTransaction;
  console.log(`Handler: Received transaction with id: ${tx.transactionId.valueOf()}`);
  return true;
}
