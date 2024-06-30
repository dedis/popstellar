import { ActionType, Message, MessageData, ObjectType } from 'core/network/jsonrpc/messages';
import { validateDataObject } from 'core/network/validation';
import { ProtocolError, PublicKey } from 'core/objects';

/** Result received for the Federation Authentication */
export class FederationResult implements MessageData {
  public readonly object: ObjectType = ObjectType.FEDERATION;

  public readonly action: ActionType = ActionType.FEDERATION_RESULT;

  public readonly reason?: String;

  public readonly public_key?: PublicKey;

  public readonly status: String;

  public readonly challenge: Message;

  constructor(msg: Partial<FederationResult>) {
    if (!msg.status) {
      throw new ProtocolError("Undefined 'status' parameter encountered during 'FederationResult'");
    }
    if (!msg.challenge) {
      throw new ProtocolError(
        "Undefined 'challenge' parameter encountered during 'FederationResult'",
      );
    }

    if (!msg.reason && !msg.public_key) {
      throw new ProtocolError(
        "Undefined 'reason' or 'public_key' parameter encountered during 'FederationResult'",
      );
    }

    this.status = msg.status;
    this.reason = msg.reason;
    this.public_key = msg.public_key;
    this.challenge = msg.challenge;
  }

  /**
   * Creates an FederationResult object from a given object
   * @param obj
   */
  public static fromJson(obj: any): FederationResult {
    const { errors } = validateDataObject(ObjectType.FEDERATION, ActionType.FEDERATION_RESULT, obj);
    if (errors !== null) {
      throw new ProtocolError(`Invalid federation result\n\n${errors}`);
    }

    return new FederationResult({
      reason: obj.reason,
      challenge: obj.challenge,
      status: obj.status,
      public_key: obj.public_key,
    });
  }
}
