import { Hash, PublicKey, Timestamp } from '../../Model/Objects';

export interface LaoData {
  name?: string,
  id?: Hash,
  creation?: Timestamp,
  last_modified?: Timestamp,
  organizer?: PublicKey,
  witnesses?: PublicKey[],
}
