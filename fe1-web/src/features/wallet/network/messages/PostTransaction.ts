import { ActionType, MessageData, ObjectType } from 'core/network/jsonrpc/messages';
import { Hash, ProtocolError } from 'core/objects';

import { hashTransaction } from '../DigitalCashHelper';
import { DigitalCashMessage, DigitalCashTransaction } from '../DigitalCashTransaction';

export class PostTransaction implements MessageData {
  public readonly object: ObjectType = ObjectType.TRANSACTION;

  public readonly action: ActionType = ActionType.POST;

  public readonly transactionId: Hash;

  public readonly transaction: DigitalCashTransaction;

  constructor(msg: Partial<PostTransaction>) {
    if (!msg.transaction) {
      throw new ProtocolError(
        "Undefined 'transaction' parameter encountered during 'PostTransaction'",
      );
    }
    this.transaction = msg.transaction;

    if (!msg.transactionId) {
      throw new ProtocolError(
        "Undefined 'transactionId' parameter encountered during 'PostTransaction'",
      );
    }
    if (hashTransaction(msg.transaction).valueOf() !== msg.transactionId.valueOf()) {
      throw new ProtocolError(
        'Invalid transaction hash encountered: the computed hash does not correspond to the received hash',
      );
    }
    this.transactionId = msg.transactionId;
  }

  /**
   * Creates a PostTransaction object from a given object.
   *
   * @param obj
   */
  public static fromJson(obj: any): PostTransaction {
    const message = DigitalCashMessage.fromState(obj);
    return new PostTransaction({
      transactionId: message.transactionId,
      transaction: message.transaction,
    });
  }
}
