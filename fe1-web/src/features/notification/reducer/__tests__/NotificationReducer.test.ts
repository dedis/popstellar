import { describe } from '@jest/globals';

import {
  addNotification,
  discardNotifications,
  discardAllNotifications,
  markNotificationAsRead,
  notificationReduce,
  NotificationState,
  NotificationReducerState,
  selectUnreadNotificationCount,
  NOTIFICATION_REDUCER_PATH,
  selectAllNotifications,
  getNotification,
} from '../NotificationReducer';

describe('NotificationReducer', () => {
  describe('addNotification', () => {
    it('adds notifications to the store', () => {
      const notification: Omit<NotificationState, 'id' | 'hasBeenRead'> = {
        title: 'some title',
        timestamp: 0,
        type: 'some-type',
      };

      const newState = notificationReduce(
        { allIds: [], byId: {}, nextId: 0 },
        addNotification(notification),
      );

      expect(newState.allIds).toEqual([0]);
      expect(newState.byId).toHaveProperty('0', { id: 0, hasBeenRead: false, ...notification });
      expect(newState.nextId).toEqual(1);
    });
  });

  describe('discardNotifications', () => {
    it('removes a notification from the store', () => {
      const newState = notificationReduce(
        {
          allIds: [0, 1],
          byId: {
            0: { id: 0, hasBeenRead: false, timestamp: 20, title: 'some title', type: 'some-type' },
            1: { id: 1, hasBeenRead: false, timestamp: 20, title: 'some title', type: 'some-type' },
          },
          nextId: 2,
        } as NotificationReducerState,
        discardNotifications([0]),
      );

      expect(newState.allIds).toEqual([1]);
      expect(newState.byId).toEqual({
        1: { id: 1, hasBeenRead: false, timestamp: 20, title: 'some title', type: 'some-type' },
      });
      expect(newState.nextId).toEqual(2);
    });

    it('removes multiple notifications from the store', () => {
      const newState = notificationReduce(
        {
          allIds: [0, 1, 2],
          byId: {
            0: { id: 0, hasBeenRead: false, timestamp: 20, title: 'some title', type: 'some-type' },
            1: { id: 1, hasBeenRead: false, timestamp: 20, title: 'some title', type: 'some-type' },
            2: { id: 2, hasBeenRead: false, timestamp: 20, title: 'some title', type: 'some-type' },
          },
          nextId: 3,
        } as NotificationReducerState,
        discardNotifications([0, 2]),
      );

      expect(newState.allIds).toEqual([1]);
      expect(newState.byId).toEqual({
        1: { id: 1, hasBeenRead: false, timestamp: 20, title: 'some title', type: 'some-type' },
      });
      expect(newState.nextId).toEqual(3);
    });

    it("doesn't do anything if the id does not exist in the store", () => {
      const newState = notificationReduce(
        {
          allIds: [0],
          byId: {
            0: { id: 0, hasBeenRead: false, timestamp: 20, title: 'some title', type: 'some-type' },
          },
          nextId: 1,
        } as NotificationReducerState,
        discardNotifications([1]),
      );

      expect(newState.allIds).toEqual([0]);
      expect(newState.byId).toEqual({
        0: { id: 0, hasBeenRead: false, timestamp: 20, title: 'some title', type: 'some-type' },
      });
      expect(newState.nextId).toEqual(1);
    });
  });

  describe('discardAllNotifications', () => {
    it('removes all notifications from the store', () => {
      const newState = notificationReduce(
        {
          allIds: [0, 10],
          byId: {
            0: { id: 0, hasBeenRead: false, timestamp: 20, title: 'some title', type: 'some-type' },
            10: {
              id: 10,
              hasBeenRead: false,
              timestamp: 20,
              title: 'some title',
              type: 'some-type',
            },
          },
          nextId: 11,
        } as NotificationReducerState,
        discardAllNotifications(),
      );

      expect(newState.allIds).toEqual([]);
      expect(newState.byId).toEqual({});
      expect(newState.nextId).toEqual(0);
    });
  });

  describe('markNotificationAsRead', () => {
    it('sets the hasBeenRead flag to true', () => {
      const notification: NotificationState = {
        id: 0,
        hasBeenRead: false,
        timestamp: 20,
        title: 'some title',
        type: 'some-type',
      };
      const newState = notificationReduce(
        {
          allIds: [0],
          byId: {
            0: notification,
          },
          nextId: 1,
        } as NotificationReducerState,
        markNotificationAsRead(0),
      );

      expect(newState.allIds).toEqual([0]);
      expect(newState.byId).toHaveProperty('0', { ...notification, hasBeenRead: true });
      expect(newState.nextId).toEqual(1);
    });

    it("doesn't do anything if the id does not exist in the store", () => {
      const newState = notificationReduce(
        {
          allIds: [0],
          byId: {
            0: { id: 0, hasBeenRead: false, timestamp: 20, title: 'some title', type: 'some-type' },
          },
          nextId: 1,
        } as NotificationReducerState,
        markNotificationAsRead(1),
      );

      expect(newState.allIds).toEqual([0]);
      expect(newState.byId).toEqual({
        0: { id: 0, hasBeenRead: false, timestamp: 20, title: 'some title', type: 'some-type' },
      });
      expect(newState.nextId).toEqual(1);
    });
  });
});

describe('selectUnreadNotificationCount', () => {
  it('returns the correct number of notifications', () => {
    expect(
      selectUnreadNotificationCount({
        [NOTIFICATION_REDUCER_PATH]: {
          allIds: [0, 1, 3, 11],
          byId: {
            0: { id: 0, hasBeenRead: true, timestamp: 20, title: 'some title', type: 'some-type' },
            1: { id: 1, hasBeenRead: false, timestamp: 20, title: 'some title', type: 'some-type' },
            3: { id: 3, hasBeenRead: true, timestamp: 20, title: 'some title', type: 'some-type' },
            11: {
              id: 11,
              hasBeenRead: false,
              timestamp: 20,
              title: 'some title',
              type: 'some-type',
            },
          },
          nextId: 12,
        },
      }),
    ).toEqual(2);
  });
});

describe('selectAllNotifications', () => {
  it('returns all notifications', () => {
    const n0 = { id: 0, hasBeenRead: true, timestamp: 20, title: 'some title', type: 'some-type' };
    const n1 = { id: 1, hasBeenRead: false, timestamp: 20, title: 'some title', type: 'some-type' };
    const n3 = { id: 3, hasBeenRead: true, timestamp: 20, title: 'some title', type: 'some-type' };
    const n11 = {
      id: 11,
      hasBeenRead: false,
      timestamp: 20,
      title: 'some title',
      type: 'some-type',
    };

    expect(
      selectAllNotifications({
        [NOTIFICATION_REDUCER_PATH]: {
          allIds: [0, 1, 3, 11],
          byId: {
            0: n0,
            1: n1,
            3: n3,
            11: n11,
          },
          nextId: 12,
        },
      }),
    ).toEqual([n0, n1, n3, n11]);
  });
});

describe('getNotification', () => {
  it('returns the correct notification', () => {
    const n0 = { id: 0, hasBeenRead: true, timestamp: 20, title: 'some title', type: 'some-type' };
    const n1 = { id: 1, hasBeenRead: false, timestamp: 20, title: 'some title', type: 'some-type' };
    const n3 = { id: 3, hasBeenRead: true, timestamp: 20, title: 'some title', type: 'some-type' };
    const n11 = {
      id: 11,
      hasBeenRead: false,
      timestamp: 20,
      title: 'some title',
      type: 'some-type',
    };

    expect(
      getNotification(3, {
        [NOTIFICATION_REDUCER_PATH]: {
          allIds: [0, 1, 3, 11],
          byId: {
            0: n0,
            1: n1,
            3: n3,
            11: n11,
          },
          nextId: 12,
        },
      }),
    ).toEqual(n3);
  });

  it('returns undefined if the notification is not in the store', () => {
    const n0 = { id: 0, hasBeenRead: true, timestamp: 20, title: 'some title', type: 'some-type' };
    const n1 = { id: 1, hasBeenRead: false, timestamp: 20, title: 'some title', type: 'some-type' };
    const n3 = { id: 3, hasBeenRead: true, timestamp: 20, title: 'some title', type: 'some-type' };
    const n11 = {
      id: 11,
      hasBeenRead: false,
      timestamp: 20,
      title: 'some title',
      type: 'some-type',
    };

    expect(
      getNotification(5, {
        [NOTIFICATION_REDUCER_PATH]: {
          allIds: [0, 1, 3, 11],
          byId: {
            0: n0,
            1: n1,
            3: n3,
            11: n11,
          },
          nextId: 12,
        },
      }),
    ).toBeUndefined();
  });
});
