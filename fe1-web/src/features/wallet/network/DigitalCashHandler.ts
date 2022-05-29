import { ActionType, ObjectType, ProcessableMessage } from 'core/network/jsonrpc/messages';
import { Base64UrlData, Hash, PublicKey } from "core/objects";

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

/**
 * Verifies the validity of the information contained in the message
 * by checking the transaction inputs signature
 * @param transactionMessage the transaction message to verify
 * @param publicKey the public key to use to verify the transaction signatures
 */
const isTransactionValid = (transactionMessage: PostTransaction, publicKey: PublicKey) => {
  const transaction = Transaction.fromJSON(
    transactionMessage.transaction,
    transactionMessage.transaction_id.valueOf(),
  );

  const dataString = Transaction.concatenateTxData(
    transaction.inputs.map((input) => input.toState()),
    transaction.outputs.map((output) => output.toState()),
  );

  return transaction.inputs.some(
    (input) => !input.script.signature.verify(publicKey, Base64UrlData.encode(dataString)),
  );
};
