import { ActionType, ObjectType, ProcessableMessage } from 'core/network/jsonrpc/messages';

import { PostTransaction } from './messages';

/**
 * Handle a PostTransaction message.
 *
 * @param msg - The message
 */
export function handleTransactionPost(msg: ProcessableMessage): boolean {
  if (
    msg.messageData.object !== ObjectType.COIN ||
    msg.messageData.action !== ActionType.POST_TRANSACTION
  ) {
    console.warn('handleTransactionPost was called to process an unsupported message', msg);
    return false;
  }
  const tx = msg.messageData as PostTransaction;
  console.log(`Handler: Received transaction with id: ${tx.transaction_id.valueOf()}`);
  return true;
}
