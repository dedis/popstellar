import { ActionType, ObjectType, ProcessableMessage } from 'core/network/jsonrpc/messages';
import { Hash, PublicKey } from 'core/objects';

import { Transaction } from '../objects/transaction';
import { DigitalCashStore } from '../store';
import { PostTransaction } from './messages';

/**
 * Handle a PostTransaction message.
 *
 * @param addTransaction - A function to add the received transaction to the digital cash state
 * @param getLaoOrganizer - A function to get the organizer from a lao
 */
export const handleTransactionPost =
  (
    addTransaction: (laoId: Hash, transaction: Transaction) => void,
    getLaoOrganizer: (laoId: string) => PublicKey | undefined,
  ) =>
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

    const organizerPublicKey = getLaoOrganizer(msg.laoId.valueOf());
    if (!organizerPublicKey) {
      console.warn('The organizer public key for this lao id was not found');
      return false;
    }

    const transaction = Transaction.fromJSON(tx.transaction, tx.transaction_id.valueOf());

    // Check the transaction signatures over the inputs and outputs,
    // and check that the transaction inputs used are consistent with our current state
    if (
      !transaction.checkTransactionValidity(
        organizerPublicKey,
        DigitalCashStore.getTransactionsById(msg.laoId.valueOf()),
      )
    ) {
      console.warn('Transaction is not valid');
      return false;
    }

    addTransaction(msg.laoId, transaction);
    return true;
  };
