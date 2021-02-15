import {
  Hash, PublicKey, Timestamp, Lao,
} from 'model/objects';
import { OpenedLaoStore } from 'store';
import { ProtocolError } from 'model/network/ProtocolError';
import { ActionType, MessageData, ObjectType } from '../MessageData';
import { checkTimestampStaleness, checkWitnesses } from '../Checker';

export class UpdateLao implements MessageData {
  public readonly object: ObjectType = ObjectType.LAO;

  public readonly action: ActionType = ActionType.UPDATE_PROPERTIES;

  public readonly id: Hash;

  public readonly name: string;

  public readonly last_modified: Timestamp;

  public readonly witnesses: PublicKey[];

  constructor(msg: Partial<UpdateLao>) {
    if (!msg.name) throw new ProtocolError('Undefined \'name\' parameter encountered during \'UpdateLao\'');
    this.name = msg.name;

    if (!msg.last_modified) throw new ProtocolError('Undefined \'last_modified\' parameter encountered during \'UpdateLao\'');
    checkTimestampStaleness(msg.last_modified);
    this.last_modified = new Timestamp(msg.last_modified.toString());

    if (!msg.witnesses) throw new ProtocolError('Undefined \'witnesses\' parameter encountered during \'UpdateLao\'');
    checkWitnesses(msg.witnesses);
    this.witnesses = msg.witnesses.map((key) => new PublicKey(key.toString()));

    if (!msg.id) throw new ProtocolError('Undefined \'id\' parameter encountered during \'UpdateLao\'');
    const lao: Lao = OpenedLaoStore.get();
    const expectedHash = Hash.fromStringArray(
      lao.organizer.toString(), lao.creation.toString(), msg.name,
    );
    if (!expectedHash.equals(msg.id)) throw new ProtocolError('Invalid \'id\' parameter encountered during \'UpdateLao\': unexpected id value');
    this.id = new Hash(msg.id.toString());
  }

  public static fromJson(obj: any): UpdateLao {
    // FIXME add JsonSchema validation to all "fromJson"
    const correctness = true;

    return correctness
      ? new UpdateLao(obj)
      : (() => { throw new ProtocolError('add JsonSchema error message'); })();
  }
}
