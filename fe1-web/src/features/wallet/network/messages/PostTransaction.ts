import { ActionType, MessageData, ObjectType } from 'core/network/jsonrpc/messages';
import { Hash, ProtocolError } from 'core/objects';

import { hashTransaction, Transaction, TransactionJSON } from '../../objects/transaction';

/**
 * A digital cash POST TRANSACTION message
 */
export class PostTransaction implements MessageData {
  public readonly object: ObjectType = ObjectType.COIN;

  public readonly action: ActionType = ActionType.POST_TRANSACTION;

  public readonly transaction_id: Hash;

  public readonly transaction: TransactionJSON;

  constructor(msg: Partial<PostTransaction>) {
    if (!msg.transaction) {
      throw new ProtocolError(
        "Undefined 'transaction' parameter encountered during 'PostTransaction'",
      );
    }
    this.transaction = msg.transaction;

    if (!msg.transaction_id) {
      throw new ProtocolError(
        "Undefined 'transaction_id' parameter encountered during 'PostTransaction'",
      );
    }
    if (
      hashTransaction(
        Transaction.fromJSON(msg.transaction, msg.transaction_id.valueOf()).toState(),
      ).valueOf() !== msg.transaction_id.valueOf()
    ) {
      throw new ProtocolError(
        'Invalid transaction hash encountered: the computed hash does not correspond to the received hash',
      );
    }
    this.transaction_id = msg.transaction_id;
  }

  /**
   * Creates a PostTransaction object from a given object.
   *
   * @param obj
   */
  public static fromJSON(obj: any): PostTransaction {
    return new PostTransaction(obj);
  }
}
