import { Hash, HashState, Timestamp, TimestampState } from 'core/objects';
import { OmitMethods } from 'core/types';

export interface NotificationState {
  id: number;
  laoId: HashState;
  hasBeenRead: boolean;
  timestamp: TimestampState;
  title: string;
  type: string;
}

export class Notification {
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
  public readonly type: string;

  constructor(notification: OmitMethods<Notification>) {
    this.id = notification.id;
    this.laoId = notification.laoId;
    this.hasBeenRead = notification.hasBeenRead;
    this.timestamp = notification.timestamp;
    this.title = notification.title;
    this.type = notification.type;
  }

  public toState(): NotificationState {
    return {
      id: this.id,
      laoId: this.laoId.toState(),
      hasBeenRead: this.hasBeenRead,
      timestamp: this.timestamp.toState(),
      title: this.title,
      type: this.type,
    };
  }

  public static fromState(notificationState: NotificationState): Notification {
    return new Notification({
      id: notificationState.id,
      laoId: Hash.fromState(notificationState.laoId),
      hasBeenRead: notificationState.hasBeenRead,
      timestamp: Timestamp.fromState(notificationState.timestamp),
      title: notificationState.title,
      type: notificationState.type,
    });
  }
}
