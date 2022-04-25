import { ActionType, MessageData, ObjectType } from 'core/network/jsonrpc/messages';
import { validateDataObject } from 'core/network/validation';
import { Hash, ProtocolError, PublicKey } from 'core/objects';
import { MessageDataProperties } from 'core/types';

/** Data received when obtaining an election key */
export class ElectionKey implements MessageData {
  public readonly object: ObjectType = ObjectType.ELECTION;

  public readonly action: ActionType = ActionType.KEY;

  public readonly election: Hash;

  public readonly electionKey: PublicKey;

  constructor(msg: MessageDataProperties<ElectionKey>) {
    if (!msg.election) {
      throw new ProtocolError("Invalid 'election' parameter encountered during 'ElectionKey'");
    }
    this.election = msg.election;

    if (!msg.electionKey) {
      throw new ProtocolError("Invalid 'electionKey' parameter encountered during 'ElectionKey'");
    }
    this.electionKey = msg.electionKey;
  }

  /**
   * Creates an ElectionKey object from a given object.
   *
   * @param obj
   */
  public static fromJson(obj: any): ElectionKey {
    const { errors } = validateDataObject(ObjectType.ELECTION, ActionType.KEY, obj);

    if (errors !== null) {
      throw new ProtocolError(`Invalid election request key\n\n${errors}`);
    }

    return new ElectionKey({
      ...obj,
      election: new Hash(obj.election),
      electionKey: new PublicKey(obj.election_key),
    });
  }
}
