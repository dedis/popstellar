import { Hash, PopToken } from 'core/objects';

export namespace SocialFeature {
  export interface Lao {
    id: Hash;
    last_tokenized_roll_call_id: Hash;
  }

  export interface RollCall {
    id: Hash;

    containsToken(token: PopToken | undefined): boolean;
  }
}
