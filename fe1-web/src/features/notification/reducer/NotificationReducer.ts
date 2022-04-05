/**
 * This error is disabled since reducers use the createSlice function, which requires the user to
 * param-reassign. Please do not disable other errors.
 */
/* eslint-disable no-param-reassign */
import { createSelector, createSlice, Draft, PayloadAction } from '@reduxjs/toolkit';

export interface NotificationState {
  /* the id of the notification, is automatically assigned */
  id: number;
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
  byId: Record<number, NotificationState>;
  allIds: number[];
  nextId: number;
}

const initialState: NotificationReducerState = {
  byId: {},
  allIds: [],
  nextId: 0,
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
      const notification: NotificationState = {
        ...action.payload,
        id: state.nextId,
        hasBeenRead: false,
      };

      state.nextId += 1;
      state.byId[notification.id] = notification;
      state.allIds.push(notification.id);
    },

    // Marks a notification as read
    markNotificationAsRead: (
      state: Draft<NotificationReducerState>,
      action: PayloadAction<number>,
    ) => {
      const notificationId = action.payload;

      if (!(notificationId in state.byId)) {
        // this message was never stored?
        return;
      }

      state.byId[notificationId].hasBeenRead = true;
    },

    // Discards a notification
    discardNotifications: (
      state: Draft<NotificationReducerState>,
      action: PayloadAction<number[]>,
    ) => {
      const notificationIds = action.payload;

      for (const notificationId of notificationIds) {
        if (!(notificationId in state.byId)) {
          // this message was never stored?
          return;
        }

        delete state.byId[notificationId];
      }

      state.allIds = state.allIds.filter((id) => !notificationIds.includes(id));
    },

    // Discards all notifications
    discardAllNotifications: (state: Draft<NotificationReducerState>) => {
      state.allIds = [];
      state.byId = {};
      state.nextId = 0;
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

export const selectUnreadNotificationCount = createSelector(
  // First input: a map containing all notifications
  (state) => getNotificationState(state).byId,
  // Second input: all notification ids
  (state) => getNotificationState(state).allIds,
  // Selector: returns the number of unread notifications
  (notificationMap: Record<string, NotificationState>, allIds: number[]): number =>
    allIds.filter((id) => !notificationMap[id].hasBeenRead).length,
);

export const selectAllNotifications = createSelector(
  // First input: a map containing all notifications
  (state) => getNotificationState(state).byId,
  // Second input: all notification ids
  (state) => getNotificationState(state).allIds,
  // Selector: returns the number of unread notifications
  (notificationMap: Record<string, NotificationState>, allIds: number[]): NotificationState[] =>
    allIds.map((id) => notificationMap[id]),
);

/**
 * Retrives a single notification state by id
 * NOTE: This function does not memoize since there is no computation taking place
 * @param notificationId The id of the notification to retrieve
 * @param state The redux state
 * @returns A single notificatio n state
 */
export const getNotification = (notificationId: number, state: unknown) => {
  const notificationState = getNotificationState(state);
  if (notificationId in notificationState.byId) {
    return notificationState.byId[notificationId];
  }

  return undefined;
};

export const notificationReduce = notificationSlice.reducer;

export default {
  [NOTIFICATION_REDUCER_PATH]: notificationSlice.reducer,
};
