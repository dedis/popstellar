import { Hash, PublicKey } from 'core/objects';

export namespace RollCallFeature {
  export interface Lao {
    id: Hash;

    witnesses: PublicKey[];
  }

  export interface Event {
    id: Hash;
    name: string;
  }
}
