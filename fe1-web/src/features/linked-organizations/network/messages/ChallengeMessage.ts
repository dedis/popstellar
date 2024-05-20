import { ActionType, MessageData, ObjectType } from 'core/network/jsonrpc/messages';
import { validateDataObject } from 'core/network/validation';
import { Hash, ProtocolError, Timestamp } from 'core/objects';

/** Data Data received when requesting a challenge */
export class ChallengeMessage implements MessageData {
  public readonly object: ObjectType = ObjectType.FEDERATION;

  public readonly action: ActionType = ActionType.CHALLENGE;

  // The challenge
  public readonly value: Hash;
  
  public readonly valid_until: Timestamp;

  constructor(msg: Partial<ChallengeMessage>) {
    if (!msg.value) {
      throw new ProtocolError("Undefined 'value' parameter encountered during 'Challenge'");
    }
    if (!msg.valid_until) {
      throw new ProtocolError("Undefined 'valid_until' parameter encountered during 'Challenge'");
    }
    this.value = msg.value;
    this.valid_until = msg.valid_until;
  }

  /**
   * Creates an ChallengeMessage object from a given object
   * @param obj
   */
  public static fromJson(obj: any): ChallengeMessage {
    const { errors } = validateDataObject(ObjectType.FEDERATION, ActionType.CHALLENGE, obj);
    if (errors !== null) {
      throw new ProtocolError(`Invalid challenge\n\n${errors}`);
    }
    return new ChallengeMessage({
      value: obj.value,
      valid_until: obj.valid_until,
    });
  }
}
