import { ActionType, MessageData, ObjectType } from 'core/network/jsonrpc/messages';
import { validateDataObject } from 'core/network/validation';
import { ProtocolError, Timestamp } from 'core/objects';

/** Data sent to request a challenge */
export class RequestChallenge implements MessageData {
  public readonly object: ObjectType = ObjectType.FEDERATION;

  public readonly action: ActionType = ActionType.CHALLENGE_REQUEST;

  // The timestamp
  public readonly timestamp: Timestamp;

  constructor(msg: Partial<RequestChallenge>) {
    if (!msg.timestamp) {
      throw new ProtocolError("Undefined 'timestamp' parameter encountered during 'RequestChallenge'");
    }
    this.timestamp = msg.timestamp;
  }

  /**
   * Creates an RequestChallenge object from a given object
   * @param obj
   */
  public static fromJson(obj: any): RequestChallenge {
    const { errors } = validateDataObject(ObjectType.FEDERATION, ActionType.CHALLENGE_REQUEST, obj);

    if (errors !== null) {
      throw new ProtocolError(`Invalid challenge request\n\n${errors}`);
    }

    return new RequestChallenge({
      timestamp: obj.timestamp,
    });
  }
}
