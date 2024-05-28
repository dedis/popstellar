import { ActionType, Message, MessageData, ObjectType } from 'core/network/jsonrpc/messages';
import { validateDataObject } from 'core/network/validation';
import { Hash, ProtocolError, PublicKey } from 'core/objects';

/** Data sent to expect Federation */
export class FederationExpect implements MessageData {
  public readonly object: ObjectType = ObjectType.FEDERATION;

  public readonly action: ActionType = ActionType.FEDERATION_EXPECT;

  // The timestamp
  public readonly lao_id: Hash;

  public readonly server_address: String;

  public readonly public_key: PublicKey;

  public readonly challenge: Message;

  constructor(msg: Partial<FederationExpect>) {
    if (!msg.lao_id) {
      throw new ProtocolError("Undefined 'lao_id' parameter encountered during 'FederationExpect'");
    }
    if (!msg.server_address) {
      throw new ProtocolError(
        "Undefined 'server_address' parameter encountered during 'FederationExpect'",
      );
    }
    if (!msg.public_key) {
      throw new ProtocolError(
        "Undefined 'public_key' parameter encountered during 'FederationExpect'",
      );
    }
    if (!msg.challenge) {
      throw new ProtocolError(
        "Undefined 'challenge' parameter encountered during 'FederationExpect'",
      );
    }
    this.lao_id = msg.lao_id;
    this.server_address = msg.server_address;
    this.public_key = msg.public_key;
    this.challenge = msg.challenge;
  }

  /**
   * Creates an FederationExpect object from a given object
   * @param obj
   */
  public static fromJson(obj: any): FederationExpect {
    const { errors } = validateDataObject(ObjectType.FEDERATION, ActionType.FEDERATION_EXPECT, obj);
    if (errors !== null) {
      throw new ProtocolError(`Invalid federation expect\n\n${errors}`);
    }

    return new FederationExpect({
      lao_id: obj.lao_id,
      server_address: obj.server_address,
      public_key: obj.public_key,
      challenge: obj.challenge,
    });
  }
}
