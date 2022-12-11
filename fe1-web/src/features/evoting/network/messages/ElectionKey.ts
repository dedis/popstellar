import { ActionType, MessageData, ObjectType } from 'core/network/jsonrpc/messages';
import { validateDataObject } from 'core/network/validation';
import { Hash, ProtocolError, PublicKey } from 'core/objects';
import { MessageDataProperties } from 'core/types';
import { ElectionPublicKey } from 'features/evoting/objects/ElectionPublicKey';

/** Data received when obtaining an election key */
export class ElectionKey implements MessageData {
  public readonly object: ObjectType = ObjectType.ELECTION;

  public readonly action: ActionType = ActionType.KEY;

  public readonly election: Hash;

  public readonly election_key: ElectionPublicKey;

  constructor(msg: MessageDataProperties<ElectionKey>) {
    if (!msg.election) {
      throw new ProtocolError("Invalid 'election' parameter encountered during 'ElectionKey'");
    }
    this.election = msg.election;

    if (!msg.election_key) {
      throw new ProtocolError("Invalid 'electionKey' parameter encountered during 'ElectionKey'");
    }
    this.election_key = msg.election_key;
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
      election_key: new PublicKey(obj.election_key),
    });
  }
}
