import { ActionType, MessageData, ObjectType } from 'core/network/jsonrpc/messages';
import { Hash, ProtocolError } from 'core/objects';

import { TransactionJSON } from '../../objects/transaction';

/**
 * A digital cash POST TRANSACTION message
 */
export class PostTransaction implements MessageData {
  public readonly object: ObjectType = ObjectType.COIN;

  public readonly action: ActionType = ActionType.POST_TRANSACTION;

  // the transaction hash of this transaction object
  public readonly transaction_id: Hash;

  // The transaction to send in the JSON format
  public readonly transaction: TransactionJSON;

  // The roll call id of this transaction
  public readonly rc_id: Hash;

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
    this.transaction_id = msg.transaction_id;

    if (!msg.rc_id) {
      throw new ProtocolError("Undefined 'rc_id' parameter encountered during 'PostTransaction'");
    }
    this.rc_id = msg.rc_id;
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
