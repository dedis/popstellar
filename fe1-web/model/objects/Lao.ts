import { Timestamp } from './Timestamp';
import { Hash } from './Hash';
import { PublicKey } from './PublicKey';

export class Lao {
  public readonly name: string;

  public readonly id: Hash;

  public readonly creation: Timestamp;

  public readonly last_modified: Timestamp;

  public readonly organizer: PublicKey;

  public readonly witnesses: PublicKey[];

  constructor(obj: Partial<Lao>) {
    if (obj === undefined || obj === null) {
      throw new Error('Error encountered while creating a LAO object : undefined/null parameters');
    }

    if (obj.name === undefined) throw new Error('Error encountered while creating a LAO object : undefined name');
    if (obj.id === undefined) throw new Error('Error encountered while creating a LAO object : undefined id');
    if (obj.creation === undefined) throw new Error('Error encountered while creating a LAO object : undefined creation');
    if (obj.last_modified === undefined) throw new Error('Error encountered while creating a LAO object : undefined last_modified');
    if (obj.organizer === undefined) throw new Error('Error encountered while creating a LAO object : undefined organizer');
    if (obj.witnesses === undefined) throw new Error('Error encountered while creating a LAO object : undefined witnesses');

    this.name = obj.name;
    this.id = obj.id;
    this.creation = obj.creation;
    this.last_modified = obj.last_modified;
    this.organizer = obj.organizer;
    this.witnesses = [...obj.witnesses];
  }
}
