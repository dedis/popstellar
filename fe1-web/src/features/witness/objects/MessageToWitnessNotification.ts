import { Hash, HashState, Timestamp } from 'core/objects';
import { OmitMethods } from 'core/types';

import { WitnessFeature } from '../interface';

export interface MessageToWitnessNotificationState extends WitnessFeature.NotificationState {
  messageId: HashState;
}

export class MessageToWitnessNotification implements WitnessFeature.Notification {
  /* the id of the notification, is automatically assigned */
  public readonly id: number;

  /* the id of the lao this notification is associated with */
  public readonly laoId: Hash;

  /* whether the notification has been read. is automatically assigned */
  public readonly hasBeenRead: boolean;

  /* the time associated with the notification */
  public readonly timestamp: Timestamp;

  /* the title that is shown in the notification */
  public readonly title: string;

  /* this field can be used to differentiate various types of notifications */
  public readonly type: WitnessFeature.NotificationTypes.MESSAGE_TO_WITNESS;

  /* The id of the message that is to be witnessed */
  public readonly messageId: Hash;

  constructor(notification: OmitMethods<MessageToWitnessNotification>) {
    this.id = notification.id;
    this.laoId = notification.laoId;
    this.hasBeenRead = notification.hasBeenRead;
    this.timestamp = notification.timestamp;
    this.title = notification.title;
    this.type = notification.type;
    this.messageId = notification.messageId;
  }

  public toState(): MessageToWitnessNotificationState {
    return {
      id: this.id,
      laoId: this.laoId.toState(),
      hasBeenRead: this.hasBeenRead,
      timestamp: this.timestamp.toState(),
      title: this.title,
      type: this.type,
      messageId: this.messageId.toState(),
    };
  }

  public static fromState(
    notificationState: WitnessFeature.NotificationState | MessageToWitnessNotificationState,
  ): MessageToWitnessNotification {
    if (notificationState.type !== WitnessFeature.NotificationTypes.MESSAGE_TO_WITNESS) {
      throw new Error(
        `Cannot call MessageToWitnessNotification.fromState() on notification with type '${notificationState.type}'`,
      );
    }

    if (!('messageId' in notificationState)) {
      throw new Error(
        `'messageId' is missing in object passed to MessageToWitnessNotification.fromState`,
      );
    }

    return new MessageToWitnessNotification({
      id: notificationState.id,
      laoId: Hash.fromState(notificationState.laoId),
      hasBeenRead: notificationState.hasBeenRead,
      timestamp: Timestamp.fromState(notificationState.timestamp),
      title: notificationState.title,
      type: notificationState.type,
      messageId: Hash.fromState(notificationState.messageId),
    });
  }
}
