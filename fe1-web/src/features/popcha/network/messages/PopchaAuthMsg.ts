import { ActionType, MessageData, ObjectType } from 'core/network/jsonrpc/messages';
import { validateDataObject } from 'core/network/validation';
import { Hash, ProtocolError } from 'core/objects';

/**
 * Data sent to authenticate a user
 */
export class PopchaAuthMsg implements MessageData {
  public readonly object: ObjectType = ObjectType.POPCHA;

  public readonly action: ActionType = ActionType.AUTH;

  public readonly client_id: string;

  public readonly nonce: string;

  public readonly identifier: Hash;

  public readonly identifier_proof: Hash;

  public readonly state: string | undefined;

  public readonly response_mode: string | undefined;

  public readonly popcha_address: string;

  constructor(msg: Partial<PopchaAuthMsg>) {
    if (!msg.nonce) {
      throw new ProtocolError("Undefined 'nonce' parameter encountered during 'PopchaAuthMsg'");
    }
    this.nonce = msg.nonce;

    if (!msg.client_id) {
      throw new ProtocolError("Undefined 'client_id' parameter encountered during 'PopchaAuthMsg'");
    }
    this.client_id = msg.client_id;

    if (!msg.identifier) {
      throw new ProtocolError(
        "Undefined 'identifier_' parameter encountered during 'PopchaAuthMsg'",
      );
    }
    this.identifier = msg.identifier;

    if (!msg.identifier_proof) {
      throw new ProtocolError(
        "Undefined 'identifier_proof' parameter encountered during 'PopchaAuthMsg'",
      );
    }
    this.identifier_proof = msg.identifier_proof;

    if (!msg.popcha_address) {
      throw new ProtocolError(
        "Undefined 'popcha_address' parameter encountered during 'PopchaAuthMsg'",
      );
    }
    this.popcha_address = msg.popcha_address;

    // optional parameters
    if (msg.state) {
      this.state = msg.state;
    }

    if (msg.response_mode) {
      this.response_mode = msg.response_mode;
    }
  }

  public static fromJson(msg: any): PopchaAuthMsg {
    // TODO: for now now json to validate to

    const { errors } = validateDataObject(ObjectType.POPCHA, ActionType.AUTH, msg);

    if (errors !== null) {
      throw new ProtocolError(`Invalid 'PopchaAuthMsg' received: ${errors}`);
    }

    console.log('PopchaAuthMsg.fromJson: ', msg);
    return new PopchaAuthMsg({ ...msg });
  }
}
