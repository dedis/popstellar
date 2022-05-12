import { Hash, PublicKey } from 'core/objects';

export namespace MeetingFeature {
  export interface Lao {
    id: Hash;

    witnesses: PublicKey[];
  }

  export interface Event {
    id: Hash;
    name: string;
  }
}
