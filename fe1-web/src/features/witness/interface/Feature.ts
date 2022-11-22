import { Hash, HashState, PublicKey, Timestamp, TimestampState } from 'core/objects';

export namespace WitnessFeature {
  export interface Lao {
    id: Hash;
    organizer: PublicKey;
    witnesses: PublicKey[];
  }

  export enum NotificationTypes {
    MESSAGE_TO_WITNESS = 'message-to-witness',
  }

  export interface Notification {
    id: number;
    laoId: Hash;
    hasBeenRead: boolean;
    timestamp: Timestamp;
    title: string;
    type: NotificationTypes;

    toState(): NotificationState;
  }

  export interface NotificationState {
    id: number;
    laoId: HashState;
    hasBeenRead: boolean;
    timestamp: TimestampState;
    title: string;
    type: NotificationTypes;
  }
}
