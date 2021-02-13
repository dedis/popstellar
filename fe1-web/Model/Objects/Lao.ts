import { Hash, Timestamp, PublicKey } from ".";

export class Lao {

  public readonly name: string;
  public readonly id: Hash;
  public readonly creation: Timestamp;
  public readonly last_modified: Timestamp;
  public readonly organizer: PublicKey;
  public readonly witnesses: PublicKey[];

  constructor(name: string, id: Hash, creation: Timestamp, last_modified: Timestamp, organizer: PublicKey, witnesses: PublicKey[]) {
    this.name = name;
    this.id = id;
    this.creation = creation;
    this.last_modified = last_modified;
    this.organizer = organizer;
    this.witnesses = witnesses;
  }
}
