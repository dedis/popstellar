import { ActionType, MessageData, ObjectType } from 'core/network/jsonrpc/messages';
import { validateDataObject } from 'core/network/validation';
import { checkTimestampStaleness } from 'core/network/validation/Checker';
import { Hash, ProtocolError, Timestamp } from 'core/objects';

const MAX_CHIRP_CHARS = 300;

/** Data sent to add a chirp */
export class AddChirp implements MessageData {
  public readonly object: ObjectType = ObjectType.CHIRP;

  public readonly action: ActionType = ActionType.ADD;

  // The text of the chirp
  public readonly text: string;

  // The parent ID of the chirp (if it is a reply)
  public readonly parent_id?: Hash;

  // The timestamp at which the chirp is posted
  public readonly timestamp: Timestamp;

  constructor(msg: Partial<AddChirp>) {
    if (!msg.text) {
      throw new ProtocolError("Undefined 'text' parameter encountered during 'AddChirp'");
    }
    if (msg.text.length > MAX_CHIRP_CHARS) {
      throw new ProtocolError('exceed maximum characters');
    }
    this.text = msg.text;

    if (!msg.timestamp) {
      throw new ProtocolError("Undefined 'timestamp' parameter encountered during 'AddChirp'");
    }
    checkTimestampStaleness(msg.timestamp);
    this.timestamp = msg.timestamp;

    if (msg.parent_id) {
      this.parent_id = msg.parent_id;
    }
  }

  /**
   * Creates an AddChirp object from a given object
   * @param obj
   */
  public static fromJson(obj: any): AddChirp {
    const { errors } = validateDataObject(ObjectType.CHIRP, ActionType.ADD, obj);

    if (errors !== null) {
      throw new ProtocolError(`Invalid chirp add\n\n${errors}`);
    }

    return new AddChirp({
      text: obj.text,
      parent_id: obj.parent_id !== undefined ? new Hash(obj.parent_id) : undefined,
      timestamp: new Timestamp(obj.timestamp),
    });
  }
}
