import { Hash, Timestamp, ProtocolError } from 'core/objects';
import { validateDataObject } from 'core/network/validation';
import { ActionType, MessageData, ObjectType } from 'core/network/jsonrpc/messages';
import { checkTimestampStaleness } from 'core/network/validation/Checker';
import { MessageDataProperties, RemoveMethods } from 'core/types';

/** Data sent to end an Election event */
export class EndElection implements MessageData {
  public readonly object: ObjectType = ObjectType.ELECTION;

  public readonly action: ActionType = ActionType.END;

  public readonly lao: Hash;

  public readonly election: Hash;

  public readonly created_at: Timestamp;

  public readonly registered_votes: Hash;

  constructor(msg: MessageDataProperties<EndElection>) {
    checkTimestampStaleness(msg.created_at);
    this.lao = msg.lao;
    this.election = msg.election;
    this.created_at = msg.created_at;
    this.registered_votes = msg.registered_votes;
  }

  /**
   * Creates an EndElection object from a given object.
   *
   * @param obj
   */
  public static fromJson(obj: any): EndElection {
    const { errors } = validateDataObject(ObjectType.ELECTION, ActionType.END, obj);

    if (errors !== null) {
      throw new ProtocolError(`Invalid election end\n\n${errors}`);
    }

    return new EndElection({
      ...obj,
      created_at: new Timestamp(obj.created_at),
      election: new Hash(obj.election),
      lao: new Hash(obj.lao),
    });
  }
}
