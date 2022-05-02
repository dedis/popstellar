/**
 * This error is disabled since reducers use the createSlice function, which requires the user to
 * param-reassign. Please do not disable other errors.
 */
/* eslint-disable no-param-reassign */
import { createSelector, createSlice, Draft, PayloadAction } from '@reduxjs/toolkit';

export interface NotificationState {
  /* the id of the notification, is automatically assigned */
  id: number;
  /* the id of the lao this notification is associated with */
  laoId: string;
  /* whether the notification has been read. is automatically assigned */
  hasBeenRead: boolean;
  /* the time associated with the notification */
  timestamp: number;
  /* the title that is shown in the notification */
  title: string;
  /* this field can be used to differentiate various types of notifications */
  type: string;
}

export const NOTIFICATION_REDUCER_PATH = 'notifications';

export interface NotificationReducerState {
  byLaoId: {
    [laoId: string]: { byId: Record<number, NotificationState>; allIds: number[]; nextId: number };
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
          allIds: [],
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
      laoState.allIds.push(notification.id);

      state.byLaoId[action.payload.laoId] = laoState;
    },

    // Marks a notification as read
    markNotificationAsRead: (
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

      state.byLaoId[laoId].byId[notificationId].hasBeenRead = true;
    },

    // Discards a set of notifications
    discardNotifications: (
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
        // remove the id from allIds
        state.byLaoId[laoId].allIds.splice(
          state.byLaoId[laoId].allIds.findIndex((id) => id === notificationId),
          1,
        );
      }
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
 * Creates a selector that returns the number of notifications for a specific lao
 * @param laoId The lao id the selector should be created for
 * @returns The selector
 */
export const makeUnreadNotificationCountSelector = (laoId: string) =>
  createSelector(
    // First input: a map containing all notifications
    (state) => getNotificationState(state).byLaoId[laoId]?.byId,
    // Second input: all notification ids
    (state) => getNotificationState(state).byLaoId[laoId]?.allIds,
    // Selector: returns the number of unread notifications
    (
      notificationMap: Record<string, NotificationState> | undefined,
      allIds: number[] | undefined,
    ): number => {
      if (!notificationMap || !allIds) {
        // no notification has been stored yet
        return 0;
      }

      return allIds.filter((id) => !notificationMap[id].hasBeenRead).length;
    },
  );

/**
 * Creates a selector that returns all notifications for a specific lao ordererd with the newest first
 * @param laoId The lao id the selector should be created for
 * @returns The selector
 */
export const makeAllNotificationsSelector = (laoId: string) =>
  createSelector(
    // First input: a map containing all notifications
    (state) => getNotificationState(state).byLaoId[laoId]?.byId,
    // Second input: all notification ids
    (state) => getNotificationState(state).byLaoId[laoId]?.allIds,
    // Selector: returns all notifications for a specific lao
    (
      notificationMap: Record<string, NotificationState> | undefined,
      allIds: number[] | undefined,
    ): NotificationState[] => {
      if (!notificationMap || !allIds) {
        return [];
      }

      const notifications = allIds.map((id) => notificationMap[id]);

      // sort in descending order, i.e. newest/latest first
      notifications.sort((a, b) => b.timestamp - a.timestamp);

      return notifications;
    },
  );

/**
 * Retrives a single notification state by id
 * NOTE: This function does not memoize since there is no computation taking place
 * @param laoId The id of the lao
 * @param notificationId The id of the notification to retrieve
 * @param state The redux state
 * @returns A single notificatio n state
 */
export const getNotification = (laoId: string, notificationId: number, state: unknown) => {
  const notificationState = getNotificationState(state);
  if (notificationState.byLaoId[laoId] && notificationId in notificationState.byLaoId[laoId].byId) {
    return notificationState.byLaoId[laoId].byId[notificationId];
  }

  return undefined;
};

export const notificationReduce = notificationSlice.reducer;

export default {
  [NOTIFICATION_REDUCER_PATH]: notificationSlice.reducer,
};
