import { ActionType, MessageData, ObjectType } from 'core/network/jsonrpc/messages';
import { validateDataObject } from 'core/network/validation';
import { Hash, ProtocolError } from 'core/objects';
import { MessageDataProperties } from 'core/types';

/** Data sent to request an election key */
export class RequestElectionKey implements MessageData {
  public readonly object: ObjectType = ObjectType.ELECTION;

  public readonly action: ActionType = ActionType.REQUEST_KEY;

  public readonly election: Hash;

  constructor(msg: MessageDataProperties<RequestElectionKey>) {
    if (!msg.election) {
      throw new ProtocolError(
        "Invalid 'election' parameter encountered during 'RequestElectionKey'",
      );
    }
    this.election = msg.election;
  }

  /**
   * Creates an RequestElectionKey object from a given object.
   *
   * @param obj
   */
  public static fromJson(obj: any): RequestElectionKey {
    const { errors } = validateDataObject(ObjectType.ELECTION, ActionType.REQUEST_KEY, obj);

    if (errors !== null) {
      throw new ProtocolError(`Invalid election request key\n\n${errors}`);
    }

    return new RequestElectionKey({
      ...obj,
      election: new Hash(obj.election),
    });
  }
}
