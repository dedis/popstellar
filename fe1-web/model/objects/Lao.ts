import { Timestamp } from './Timestamp';
import { Hash } from './Hash';
import { PublicKey } from './PublicKey';

export interface LaoState {
  name: string;
  id: string;
  creation: number;
  last_modified: number;
  organizer: string;
  witnesses: string[];
}

export class Lao {
  public readonly name: string;

  public readonly id: Hash;

  public readonly creation: Timestamp;

  public readonly last_modified: Timestamp;

  public readonly organizer: PublicKey;

  public readonly witnesses: PublicKey[];

  constructor(obj: Partial<Lao>) {
    if (obj === undefined || obj === null) {
      throw new Error('Error encountered while creating a LAO object: undefined/null parameters');
    }

    if (obj.id === undefined) {
      throw new Error("Undefined 'id' when creating 'LAO'");
    }
    if (obj.name === undefined) {
      throw new Error("Undefined 'name' when creating 'LAO'");
    }
    if (obj.creation === undefined) {
      throw new Error("Undefined 'creation' when creating 'LAO'");
    }
    if (obj.last_modified === undefined) {
      throw new Error("Undefined 'last_modified' when creating 'LAO'");
    }
    if (obj.organizer === undefined) {
      throw new Error("Undefined 'organizer' when creating 'LAO'");
    }
    if (obj.witnesses === undefined) {
      throw new Error("Undefined 'witnesses' when creating 'LAO'");
    }

    this.name = obj.name;
    this.id = obj.id;
    this.creation = obj.creation;
    this.last_modified = obj.last_modified;
    this.organizer = obj.organizer;
    this.witnesses = [...obj.witnesses];
  }

  public static fromState(lao: LaoState): Lao {
    return new Lao({
      name: lao.name,
      id: new Hash(lao.id),
      creation: new Timestamp(lao.creation),
      last_modified: new Timestamp(lao.last_modified),
      organizer: new PublicKey(lao.organizer),
      witnesses: lao.witnesses.map((w) => new PublicKey(w)),
    });
  }

  public toState(): LaoState {
    return JSON.parse(JSON.stringify(this));
  }
}
