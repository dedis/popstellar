import {
  Hash, PublicKey, Timestamp, Lao,
} from 'model/objects';
import { OpenedLaoStore } from 'store';
import { ProtocolError } from 'model/network/ProtocolError';
import { validateDataObject } from 'model/network/validation';
import { ActionType, MessageData, ObjectType } from '../MessageData';
import { checkTimestampStaleness, checkWitnesses } from '../Checker';

/** Data sent to update a Lao */
export class UpdateLao implements MessageData {
  public readonly object: ObjectType = ObjectType.LAO;

  public readonly action: ActionType = ActionType.UPDATE_PROPERTIES;

  public readonly id: Hash;

  public readonly name: string;

  public readonly last_modified: Timestamp;

  public readonly witnesses: PublicKey[];

  constructor(msg: Partial<UpdateLao>) {
    if (!msg.name) {
      throw new ProtocolError('Undefined \'name\' parameter encountered during \'UpdateLao\'');
    }
    this.name = msg.name;

    if (!msg.last_modified) {
      throw new ProtocolError('Undefined \'last_modified\' parameter encountered during \'UpdateLao\'');
    }
    checkTimestampStaleness(msg.last_modified);
    this.last_modified = msg.last_modified;

    if (!msg.witnesses) {
      throw new ProtocolError('Undefined \'witnesses\' parameter encountered during \'UpdateLao\'');
    }
    checkWitnesses(msg.witnesses);
    this.witnesses = [...msg.witnesses];

    if (!msg.id) {
      throw new ProtocolError('Undefined \'id\' parameter encountered during \'UpdateLao\'');
    }
    const lao: Lao = OpenedLaoStore.get();
    const expectedHash = Hash.fromStringArray(
      lao.organizer.toString(), lao.creation.toString(), msg.name,
    );
    if (!expectedHash.equals(msg.id)) {
      throw new ProtocolError('Invalid \'id\' parameter encountered during \'UpdateLao\': unexpected id value');
    }
    this.id = msg.id;
  }

  /**
   * Creates an UpdateLao object from a given object
   * @param obj
   */
  public static fromJson(obj: any): UpdateLao {
    const { errors } = validateDataObject(ObjectType.LAO, ActionType.UPDATE_PROPERTIES, obj);

    if (errors !== null) {
      throw new ProtocolError(`Invalid LAO update\n\n${errors}`);
    }

    return new UpdateLao({
      ...obj,
      last_modified: new Timestamp(obj.last_modified),
      witnesses: obj.witnesses.map((key: string) => new PublicKey(key)),
      id: new Hash(obj.id),
    });
  }
}
