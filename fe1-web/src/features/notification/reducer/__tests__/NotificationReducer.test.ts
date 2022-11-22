import { describe } from '@jest/globals';

import { mockLaoId, serializedMockLaoId } from '__tests__/utils';
import { NotificationState } from 'features/notification/objects/Notification';

import {
  addNotification,
  discardNotifications,
  discardAllNotifications,
  markNotificationAsRead,
  notificationReduce,
  NotificationReducerState,
  makeUnreadNotificationCountSelector,
  NOTIFICATION_REDUCER_PATH,
  makeUnreadNotificationsSelector,
  makeReadNotificationsSelector,
  makeNotificationSelector,
} from '../NotificationReducer';

const n0 = {
  id: 0,
  laoId: serializedMockLaoId,
  hasBeenRead: true,
  timestamp: 20,
  title: 'some title',
  type: 'some-type',
} as NotificationState;

const n1 = {
  id: 1,
  laoId: serializedMockLaoId,
  hasBeenRead: false,
  timestamp: 20,
  title: 'some title',
  type: 'some-type',
} as NotificationState;

const n3 = {
  id: 3,
  laoId: serializedMockLaoId,
  hasBeenRead: true,
  timestamp: 20,
  title: 'some title',
  type: 'some-type',
} as NotificationState;

const n11 = {
  id: 11,
  laoId: serializedMockLaoId,
  hasBeenRead: false,
  timestamp: 20,
  title: 'some title',
  type: 'some-type',
} as NotificationState;

describe('NotificationReducer', () => {
  describe('addNotification', () => {
    it('adds notifications to the store', () => {
      const notification = {
        title: 'some title',
        laoId: mockLaoId.toState(),
        timestamp: 0,
        type: 'some-type',
      };

      const newState = notificationReduce({ byLaoId: {} }, addNotification(notification));

      expect(newState.byLaoId[serializedMockLaoId].unreadIds).toEqual([0]);
      expect(newState.byLaoId[serializedMockLaoId].readIds).toEqual([]);
      expect(newState.byLaoId[serializedMockLaoId].byId).toHaveProperty('0', {
        id: 0,
        hasBeenRead: false,
        ...notification,
      });
      expect(newState.byLaoId[serializedMockLaoId].nextId).toEqual(1);
    });
  });

  describe('discardNotifications', () => {
    it('removes a notification from the store', () => {
      const newState = notificationReduce(
        {
          byLaoId: {
            [serializedMockLaoId]: {
              unreadIds: [0, 1],
              readIds: [],
              byId: {
                0: n0,
                1: n1,
              },
              nextId: 2,
            },
          },
        } as NotificationReducerState,
        discardNotifications({ laoId: mockLaoId, notificationIds: [0] }),
      );

      expect(newState.byLaoId[serializedMockLaoId].unreadIds).toEqual([1]);
      expect(newState.byLaoId[serializedMockLaoId].readIds).toEqual([]);
      expect(newState.byLaoId[serializedMockLaoId].byId).toEqual({
        1: n1,
      });
      expect(newState.byLaoId[serializedMockLaoId].nextId).toEqual(2);
    });

    it('removes multiple notifications from the store', () => {
      const newState = notificationReduce(
        {
          byLaoId: {
            [serializedMockLaoId]: {
              unreadIds: [0, 1],
              readIds: [3],
              byId: {
                0: n0,
                1: n1,
                3: n3,
              },
              nextId: 4,
            },
          },
        } as NotificationReducerState,
        discardNotifications({ laoId: mockLaoId, notificationIds: [0, 3] }),
      );

      expect(newState.byLaoId[serializedMockLaoId].unreadIds).toEqual([1]);
      expect(newState.byLaoId[serializedMockLaoId].readIds).toEqual([]);
      expect(newState.byLaoId[serializedMockLaoId].byId).toEqual({
        1: n1,
      });
      expect(newState.byLaoId[serializedMockLaoId].nextId).toEqual(4);
    });

    it("doesn't do anything if the id does not exist in the store", () => {
      const newState = notificationReduce(
        {
          byLaoId: {
            [serializedMockLaoId]: {
              unreadIds: [0],
              readIds: [],
              byId: {
                0: n0,
              },
              nextId: 1,
            },
          },
        } as NotificationReducerState,
        discardNotifications({ laoId: mockLaoId, notificationIds: [1] }),
      );

      expect(newState.byLaoId[serializedMockLaoId].unreadIds).toEqual([0]);
      expect(newState.byLaoId[serializedMockLaoId].readIds).toEqual([]);
      expect(newState.byLaoId[serializedMockLaoId].byId).toEqual({
        0: n0,
      });
      expect(newState.byLaoId[serializedMockLaoId].nextId).toEqual(1);
    });
  });

  describe('discardAllNotifications', () => {
    it('removes all notifications for a given lao from the store', () => {
      const otherMockLaoId = 'some other id';

      const newState = notificationReduce(
        {
          byLaoId: {
            [serializedMockLaoId]: {
              unreadIds: [10],
              readIds: [0],
              byId: {
                0: n0,
                10: n11,
              },
              nextId: 12,
            },
            [otherMockLaoId]: {
              unreadIds: [],
              readIds: [0],
              byId: {
                0: {
                  id: 0,
                  laoId: otherMockLaoId,
                  hasBeenRead: false,
                  timestamp: 13,
                  title: 'some title',
                  type: 'some-type',
                },
              },
              nextId: 1,
            },
          },
        } as NotificationReducerState,
        discardAllNotifications(serializedMockLaoId),
      );

      expect(newState.byLaoId).toEqual({
        [otherMockLaoId]: {
          unreadIds: [],
          readIds: [0],
          byId: {
            0: {
              id: 0,
              laoId: otherMockLaoId,
              hasBeenRead: false,
              timestamp: 13,
              title: 'some title',
              type: 'some-type',
            },
          },
          nextId: 1,
        },
      } as NotificationReducerState['byLaoId']);
    });
  });

  describe('markNotificationAsRead', () => {
    it('sets the hasBeenRead flag to true', () => {
      const notification: NotificationState = {
        id: 0,
        laoId: serializedMockLaoId,
        hasBeenRead: false,
        timestamp: 20,
        title: 'some title',
        type: 'some-type',
      };
      const newState = notificationReduce(
        {
          byLaoId: {
            [serializedMockLaoId]: {
              unreadIds: [0],
              readIds: [],
              byId: {
                0: notification,
              },
              nextId: 1,
            },
          },
        } as NotificationReducerState,
        markNotificationAsRead({ laoId: mockLaoId, notificationId: 0 }),
      );

      expect(newState.byLaoId[serializedMockLaoId].unreadIds).toEqual([]);
      expect(newState.byLaoId[serializedMockLaoId].readIds).toEqual([0]);
      expect(newState.byLaoId[serializedMockLaoId].byId).toHaveProperty('0', {
        ...notification,
        hasBeenRead: true,
      });
      expect(newState.byLaoId[serializedMockLaoId].nextId).toEqual(1);
    });

    it("doesn't do anything if the id does not exist in the store", () => {
      const newState = notificationReduce(
        {
          byLaoId: {
            [serializedMockLaoId]: {
              unreadIds: [0],
              readIds: [],
              byId: {
                0: n0,
              },
              nextId: 1,
            },
          },
        } as NotificationReducerState,
        markNotificationAsRead({ laoId: mockLaoId, notificationId: 1 }),
      );

      expect(newState.byLaoId[serializedMockLaoId].unreadIds).toEqual([0]);
      expect(newState.byLaoId[serializedMockLaoId].readIds).toEqual([]);
      expect(newState.byLaoId[serializedMockLaoId].byId).toEqual({
        0: n0,
      });
      expect(newState.byLaoId[serializedMockLaoId].nextId).toEqual(1);
    });
  });
});

describe('makeUnreadNotificationCountSelector', () => {
  it('returns the correct number of notifications', () => {
    expect(
      makeUnreadNotificationCountSelector(mockLaoId)({
        [NOTIFICATION_REDUCER_PATH]: {
          byLaoId: {
            [serializedMockLaoId]: {
              unreadIds: [1, 11],
              readIds: [0, 3],
              byId: {
                0: n0,
                1: n1,
                3: n3,
                11: n11,
              },
              nextId: 12,
            },
          },
        } as NotificationReducerState,
      }),
    ).toEqual(2);
  });
});

describe('makeUnreadNotificationsSelector', () => {
  it('returns all read notifications', () => {
    expect(
      makeUnreadNotificationsSelector(mockLaoId)({
        [NOTIFICATION_REDUCER_PATH]: {
          byLaoId: {
            [serializedMockLaoId]: {
              unreadIds: [1, 11],
              readIds: [0, 3],
              byId: {
                0: n0,
                1: n1,
                3: n3,
                11: n11,
              },
              nextId: 12,
            },
          },
        } as NotificationReducerState,
      }),
    ).toEqual([n1, n11]);
  });
});

describe('makeReadNotificationsSelector', () => {
  it('returns all read notifications', () => {
    expect(
      makeReadNotificationsSelector(mockLaoId)({
        [NOTIFICATION_REDUCER_PATH]: {
          byLaoId: {
            [serializedMockLaoId]: {
              unreadIds: [1, 11],
              readIds: [0, 3],
              byId: {
                0: n0,
                1: n1,
                3: n3,
                11: n11,
              },
              nextId: 12,
            },
          },
        } as NotificationReducerState,
      }),
    ).toEqual([n0, n3]);
  });
});

describe('makeNotificationSelector', () => {
  it('returns the correct notification', () => {
    expect(
      makeNotificationSelector(
        mockLaoId,
        3,
      )({
        [NOTIFICATION_REDUCER_PATH]: {
          byLaoId: {
            [serializedMockLaoId]: {
              unreadIds: [1, 11],
              readIds: [0, 3],
              byId: {
                0: n0,
                1: n1,
                3: n3,
                11: n11,
              },
              nextId: 12,
            },
          },
        } as NotificationReducerState,
      }),
    ).toEqual(n3);
  });

  it('returns undefined if the notification is not in the store', () => {
    expect(
      makeNotificationSelector(
        mockLaoId,
        5,
      )({
        [NOTIFICATION_REDUCER_PATH]: {
          byLaoId: {
            [serializedMockLaoId]: {
              unreadIds: [1, 11],
              readIds: [0, 3],
              byId: {
                0: n0,
                1: n1,
                3: n3,
                11: n11,
              },
              nextId: 12,
            },
          },
        } as NotificationReducerState,
      }),
    ).toBeUndefined();
  });
});
