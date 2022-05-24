import { Hash, PublicKey } from 'core/objects';

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
    laoId: string;
    hasBeenRead: boolean;
    timestamp: number;
    title: string;
    type: NotificationTypes;
  }

  export interface MessageToWitnessNotification extends Notification {
    messageId: string;
  }
}
