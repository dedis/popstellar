import { ActionType, MessageData, ObjectType } from 'core/network/jsonrpc/messages';
import { Hash, ProtocolError } from 'core/objects';

import { hashTransaction } from '../DigitalCashHelper';
import { DigitalCashMessage, DigitalCashTransaction } from '../DigitalCashTransaction';

export class PostTransaction implements MessageData {
  public readonly object: ObjectType = ObjectType.TRANSACTION;

  public readonly action: ActionType = ActionType.POST;

  public readonly transaction_id: Hash;

  public readonly transaction: DigitalCashTransaction;

  constructor(msg: Partial<PostTransaction>) {
    if (!msg.transaction) {
      throw new ProtocolError(
        "Undefined 'transaction' parameter encountered during 'PostTransaction'",
      );
    }
    this.transaction = msg.transaction;

    if (!msg.transaction_id) {
      throw new ProtocolError(
        "Undefined 'transactionId' parameter encountered during 'PostTransaction'",
      );
    }
    if (hashTransaction(msg.transaction).valueOf() !== msg.transaction_id.valueOf()) {
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
  public static fromJson(obj: any): PostTransaction {
    const messageObj = {
      ...obj,
      transactionId: obj.transaction_id,
    };
    const message = DigitalCashMessage.fromState(messageObj);
    return new PostTransaction({
      transaction_id: message.transactionId,
      transaction: message.transaction,
    });
  }
}
