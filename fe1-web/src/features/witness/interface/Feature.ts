import { Hash, PublicKey } from 'core/objects';

export namespace WitnessFeature {
  export interface Lao {
    id: Hash;
    organizer: PublicKey;
    witnesses: PublicKey[];
  }
}
