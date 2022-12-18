/**
 * This error is disabled since reducers use the createSlice function, which requires the user to
 * param-reassign. Please do not disable other errors.
 */
/* eslint-disable no-param-reassign */
import { createSelector, createSlice, Draft, PayloadAction } from '@reduxjs/toolkit';

import { Hash } from 'core/objects';

import { NotificationState } from '../objects/Notification';

export const NOTIFICATION_REDUCER_PATH = 'notifications';

export interface NotificationReducerState {
  byLaoId: {
    [laoId: string]: {
      byId: Record<number, NotificationState>;
      unreadIds: number[];
      readIds: number[];
      nextId: number;
    };
  };
}

const initialState: NotificationReducerState = {
  byLaoId: {},
};

const notificationSlice = createSlice({
  name: NOTIFICATION_REDUCER_PATH,
  initialState,
  reducers: {
    // Action called to add a new notification
    // cannot trivially accept a class instance because id and hasBeenRead
    // will be set automatically
    addNotification: (
      state: Draft<NotificationReducerState>,
      action: PayloadAction<Omit<NotificationState, 'id' | 'hasBeenRead'>>,
    ) => {
      // check if there are already notifications for this lao
      let laoState = state.byLaoId[action.payload.laoId];
      if (!laoState) {
        // if not create a new, empty state for that lao
        laoState = {
          byId: {},
          unreadIds: [],
          readIds: [],
          nextId: 0,
        };
      }

      const notification: NotificationState = {
        ...action.payload,
        id: laoState.nextId,
        hasBeenRead: false,
      };

      laoState.nextId += 1;
      laoState.byId[notification.id] = notification;
      laoState.unreadIds.push(notification.id);

      state.byLaoId[action.payload.laoId] = laoState;
    },

    // Marks a notification as read
    markNotificationAsRead: {
      prepare: (action: { laoId: Hash; notificationId: number }) => {
        return {
          payload: {
            laoId: action.laoId.valueOf(),
            notificationId: action.notificationId,
          },
        };
      },
      reducer: (
        state: Draft<NotificationReducerState>,
        action: PayloadAction<{ laoId: string; notificationId: number }>,
      ) => {
        const { laoId, notificationId } = action.payload;

        if (!(laoId in state.byLaoId)) {
          console.warn(
            `Tried to mark the notification with id ${notificationId} in lao ${laoId} as read but no notifications have been stored`,
          );
          return;
        }

        if (!(notificationId in state.byLaoId[laoId].byId)) {
          console.warn(
            `Tried to mark the notification with id ${notificationId} as read but this notification id has never been stored`,
          );
          return;
        }

        if (state.byLaoId[laoId].byId[notificationId].hasBeenRead) {
          console.warn(`The notification with id ${notificationId} was already marked as read`);
          return;
        }

        state.byLaoId[laoId].byId[notificationId].hasBeenRead = true;
        state.byLaoId[laoId].unreadIds = state.byLaoId[laoId].unreadIds.filter(
          (id) => id !== notificationId,
        );
        state.byLaoId[laoId].readIds.push(notificationId);
      },
    },

    // Discards a set of notifications
    discardNotifications: {
      prepare: (action: { laoId: Hash; notificationIds: number[] }) => {
        return {
          payload: {
            laoId: action.laoId.valueOf(),
            notificationIds: action.notificationIds,
          },
        };
      },
      reducer: (
        state: Draft<NotificationReducerState>,
        action: PayloadAction<{ laoId: string; notificationIds: number[] }>,
      ) => {
        const { laoId, notificationIds } = action.payload;

        if (!(laoId in state.byLaoId)) {
          console.warn(
            `Tried to discard the notifications with ids ${notificationIds.join(
              ', ',
            )} in lao ${laoId} as read but no notifications have been stored`,
          );
          return;
        }

        for (const notificationId of notificationIds) {
          if (!(notificationId in state.byLaoId[laoId].byId)) {
            console.warn(
              `Tried to discard the notification with id ${notificationId} but this notification id has never been stored`,
            );
            return;
          }

          delete state.byLaoId[laoId].byId[notificationId];
        }

        state.byLaoId[laoId].unreadIds = state.byLaoId[laoId].unreadIds.filter(
          (id) => !notificationIds.includes(id),
        );
        state.byLaoId[laoId].readIds = state.byLaoId[laoId].readIds.filter(
          (id) => !notificationIds.includes(id),
        );
      },
    },

    // Discards all notifications
    discardAllNotifications: (
      state: Draft<NotificationReducerState>,
      action: PayloadAction<string>,
    ) => {
      const laoId = action.payload;

      if (!(laoId in state.byLaoId)) {
        // there was no notification for this lao anyway
        return;
      }

      delete state.byLaoId[laoId];
    },
  },
});

export const {
  addNotification,
  discardNotifications,
  discardAllNotifications,
  markNotificationAsRead,
} = notificationSlice.actions;

export const getNotificationState = (state: any): NotificationReducerState =>
  state[NOTIFICATION_REDUCER_PATH];

/**
 * Creates a selector that returns the number of unread notifications for a specific lao
 * @param laoId The lao id the selector should be created for
 * @returns The selector
 */
export const makeUnreadNotificationCountSelector = (laoId: Hash) => {
  const serializedLaoId = laoId.valueOf();

  return createSelector(
    // First input: all notification ids
    (state: any) => getNotificationState(state).byLaoId[serializedLaoId]?.unreadIds,
    // Selector: returns the number of unread notifications
    (unreadIds: number[] | undefined): number => {
      if (!unreadIds) {
        return 0;
      }

      return unreadIds.length;
    },
  );
};

/**
 * Creates a selector that returns all unread notifications for a specific lao ordererd with the newest first
 * @param laoId The lao id the selector should be created for
 * @returns The selector
 */
export const makeUnreadNotificationsSelector = (laoId: Hash) => {
  const serializedLaoId = laoId.valueOf();

  return createSelector(
    // First input: a map containing all notifications
    (state: any) => getNotificationState(state).byLaoId[serializedLaoId]?.byId,
    // Second input: all ids of unread notifications
    (state: any) => getNotificationState(state).byLaoId[serializedLaoId]?.unreadIds,
    // Selector: returns all unread notifications for a specific lao
    (
      notificationMap: Record<string, NotificationState> | undefined,
      unreadIds: number[] | undefined,
    ): NotificationState[] => {
      if (!notificationMap || !unreadIds) {
        return [];
      }

      const notifications = unreadIds.map((id) => notificationMap[id]);

      // sort in descending order, i.e. newest/latest first
      notifications.sort((a, b) => b.timestamp - a.timestamp);

      return notifications;
    },
  );
};

/**
 * Creates a selector that returns all read notifications for a specific lao ordererd with the newest first
 * @param laoId The lao id the selector should be created for
 * @returns The selector
 */
export const makeReadNotificationsSelector = (laoId: Hash) => {
  const serializedLaoId = laoId.valueOf();

  return createSelector(
    // First input: a map containing all notifications
    (state: any) => getNotificationState(state).byLaoId[serializedLaoId]?.byId,
    // Second input: all ids of read notifications
    (state: any) => getNotificationState(state).byLaoId[serializedLaoId]?.readIds,
    // Selector: returns all read notifications for a specific lao
    (
      notificationMap: Record<string, NotificationState> | undefined,
      readIds: number[] | undefined,
    ): NotificationState[] => {
      if (!notificationMap || !readIds) {
        return [];
      }

      const notifications = readIds.map((id) => notificationMap[id]);

      // sort in descending order, i.e. newest/latest first
      notifications.sort((a, b) => b.timestamp - a.timestamp);

      return notifications;
    },
  );
};

/**
 * Retrives a single notification state by id
 * @param laoId The id of the lao
 * @param notificationId The id of the notification to retrieve
 * @returns A single notification state
 */
export const makeNotificationSelector = (laoId: Hash, notificationId: number) =>
  createSelector(
    // First input: a map containing all notifications
    (state: any) => getNotificationState(state),
    // Selector: returns the notification for a specific lao and notification id
    (notificationState: NotificationReducerState): NotificationState | undefined => {
      const serializedLaoId = laoId.valueOf();

      return notificationState.byLaoId[serializedLaoId]?.byId[notificationId];
    },
  );

export const notificationReduce = notificationSlice.reducer;

export default {
  [NOTIFICATION_REDUCER_PATH]: notificationSlice.reducer,
};
