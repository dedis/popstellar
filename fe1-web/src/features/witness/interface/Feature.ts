import { Hash, PublicKey } from 'core/objects';

export const MESSAGE_TO_WITNESS_NOTIFICATION_TYPE = 'message-to-witness';

export namespace WitnessFeature {
  export interface Lao {
    id: Hash;
    organizer: PublicKey;
    witnesses: PublicKey[];
  }

  export interface Notification {
    id: number;
    timestamp: number;
    type: string;
  }

  export interface MessageToWitnessNotification extends Notification {
    messageId: string;
  }
}
