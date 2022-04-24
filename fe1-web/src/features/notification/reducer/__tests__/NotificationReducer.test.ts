import { describe } from '@jest/globals';

import { mockLaoId } from '__tests__/utils';

import {
  addNotification,
  discardNotifications,
  discardAllNotifications,
  markNotificationAsRead,
  notificationReduce,
  NotificationState,
  NotificationReducerState,
  makeUnreadNotificationCountSelector,
  NOTIFICATION_REDUCER_PATH,
  makeAllNotificationsSelector,
  getNotification,
} from '../NotificationReducer';

describe('NotificationReducer', () => {
  describe('addNotification', () => {
    it('adds notifications to the store', () => {
      const notification: Omit<NotificationState, 'id' | 'hasBeenRead'> = {
        title: 'some title',
        laoId: mockLaoId,
        timestamp: 0,
        type: 'some-type',
      };

      const newState = notificationReduce({ byLaoId: {} }, addNotification(notification));

      expect(newState.byLaoId[mockLaoId].allIds).toEqual([0]);
      expect(newState.byLaoId[mockLaoId].byId).toHaveProperty('0', {
        id: 0,
        hasBeenRead: false,
        ...notification,
      });
      expect(newState.byLaoId[mockLaoId].nextId).toEqual(1);
    });
  });

  describe('discardNotifications', () => {
    it('removes a notification from the store', () => {
      const newState = notificationReduce(
        {
          byLaoId: {
            [mockLaoId]: {
              allIds: [0, 1],
              byId: {
                0: {
                  id: 0,
                  laoId: mockLaoId,
                  hasBeenRead: false,
                  timestamp: 20,
                  title: 'some title',
                  type: 'some-type',
                },
                1: {
                  id: 1,
                  laoId: mockLaoId,
                  hasBeenRead: false,
                  timestamp: 20,
                  title: 'some title',
                  type: 'some-type',
                },
              },
              nextId: 2,
            },
          },
        } as NotificationReducerState,
        discardNotifications({ laoId: mockLaoId, notificationIds: [0] }),
      );

      expect(newState.byLaoId[mockLaoId].allIds).toEqual([1]);
      expect(newState.byLaoId[mockLaoId].byId).toEqual({
        1: {
          id: 1,
          laoId: mockLaoId,
          hasBeenRead: false,
          timestamp: 20,
          title: 'some title',
          type: 'some-type',
        },
      });
      expect(newState.byLaoId[mockLaoId].nextId).toEqual(2);
    });

    it('removes multiple notifications from the store', () => {
      const newState = notificationReduce(
        {
          byLaoId: {
            [mockLaoId]: {
              allIds: [0, 1, 2],
              byId: {
                0: {
                  id: 0,
                  laoId: mockLaoId,
                  hasBeenRead: false,
                  timestamp: 20,
                  title: 'some title',
                  type: 'some-type',
                },
                1: {
                  id: 1,
                  laoId: mockLaoId,
                  hasBeenRead: false,
                  timestamp: 20,
                  title: 'some title',
                  type: 'some-type',
                },
                2: {
                  id: 2,
                  laoId: mockLaoId,
                  hasBeenRead: false,
                  timestamp: 20,
                  title: 'some title',
                  type: 'some-type',
                },
              },
              nextId: 3,
            },
          },
        } as NotificationReducerState,
        discardNotifications({ laoId: mockLaoId, notificationIds: [0, 2] }),
      );

      expect(newState.byLaoId[mockLaoId].allIds).toEqual([1]);
      expect(newState.byLaoId[mockLaoId].byId).toEqual({
        1: {
          id: 1,
          laoId: mockLaoId,
          hasBeenRead: false,
          timestamp: 20,
          title: 'some title',
          type: 'some-type',
        },
      });
      expect(newState.byLaoId[mockLaoId].nextId).toEqual(3);
    });

    it("doesn't do anything if the id does not exist in the store", () => {
      const newState = notificationReduce(
        {
          byLaoId: {
            [mockLaoId]: {
              allIds: [0],
              byId: {
                0: {
                  id: 0,
                  laoId: mockLaoId,
                  hasBeenRead: false,
                  timestamp: 20,
                  title: 'some title',
                  type: 'some-type',
                },
              },
              nextId: 1,
            },
          },
        } as NotificationReducerState,
        discardNotifications({ laoId: mockLaoId, notificationIds: [1] }),
      );

      expect(newState.byLaoId[mockLaoId].allIds).toEqual([0]);
      expect(newState.byLaoId[mockLaoId].byId).toEqual({
        0: {
          id: 0,
          laoId: mockLaoId,
          hasBeenRead: false,
          timestamp: 20,
          title: 'some title',
          type: 'some-type',
        } as NotificationState,
      });
      expect(newState.byLaoId[mockLaoId].nextId).toEqual(1);
    });
  });

  describe('discardAllNotifications', () => {
    it('removes all notifications for a given lao from the store', () => {
      const otherMockLaoId = 'some other id';

      const newState = notificationReduce(
        {
          byLaoId: {
            [mockLaoId]: {
              allIds: [0, 10],
              byId: {
                0: {
                  id: 0,
                  laoId: mockLaoId,
                  hasBeenRead: false,
                  timestamp: 20,
                  title: 'some title',
                  type: 'some-type',
                },
                10: {
                  id: 10,
                  laoId: mockLaoId,
                  hasBeenRead: false,
                  timestamp: 20,
                  title: 'some title',
                  type: 'some-type',
                },
              },
              nextId: 11,
            },
            [otherMockLaoId]: {
              allIds: [0],
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
        discardAllNotifications(mockLaoId),
      );

      expect(newState.byLaoId).toEqual({
        [otherMockLaoId]: {
          allIds: [0],
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
        laoId: mockLaoId,
        hasBeenRead: false,
        timestamp: 20,
        title: 'some title',
        type: 'some-type',
      };
      const newState = notificationReduce(
        {
          byLaoId: {
            [mockLaoId]: {
              allIds: [0],
              byId: {
                0: notification,
              },
              nextId: 1,
            },
          },
        } as NotificationReducerState,
        markNotificationAsRead({ laoId: mockLaoId, notificationId: 0 }),
      );

      expect(newState.byLaoId[mockLaoId].allIds).toEqual([0]);
      expect(newState.byLaoId[mockLaoId].byId).toHaveProperty('0', {
        ...notification,
        hasBeenRead: true,
      });
      expect(newState.byLaoId[mockLaoId].nextId).toEqual(1);
    });

    it("doesn't do anything if the id does not exist in the store", () => {
      const newState = notificationReduce(
        {
          byLaoId: {
            [mockLaoId]: {
              allIds: [0],
              byId: {
                0: {
                  id: 0,
                  laoId: mockLaoId,
                  hasBeenRead: false,
                  timestamp: 20,
                  title: 'some title',
                  type: 'some-type',
                },
              },
              nextId: 1,
            },
          },
        } as NotificationReducerState,
        markNotificationAsRead({ laoId: mockLaoId, notificationId: 1 }),
      );

      expect(newState.byLaoId[mockLaoId].allIds).toEqual([0]);
      expect(newState.byLaoId[mockLaoId].byId).toEqual({
        0: {
          id: 0,
          laoId: mockLaoId,
          hasBeenRead: false,
          timestamp: 20,
          title: 'some title',
          type: 'some-type',
        } as NotificationState,
      });
      expect(newState.byLaoId[mockLaoId].nextId).toEqual(1);
    });
  });
});

describe('makeUnreadNotificationCountSelector', () => {
  it('returns the correct number of notifications', () => {
    expect(
      makeUnreadNotificationCountSelector(mockLaoId)({
        [NOTIFICATION_REDUCER_PATH]: {
          byLaoId: {
            [mockLaoId]: {
              allIds: [0, 1, 3, 11],
              byId: {
                0: {
                  id: 0,
                  laoId: mockLaoId,
                  hasBeenRead: true,
                  timestamp: 20,
                  title: 'some title',
                  type: 'some-type',
                },
                1: {
                  id: 1,
                  laoId: mockLaoId,
                  hasBeenRead: false,
                  timestamp: 20,
                  title: 'some title',
                  type: 'some-type',
                },
                3: {
                  id: 3,
                  laoId: mockLaoId,
                  hasBeenRead: true,
                  timestamp: 20,
                  title: 'some title',
                  type: 'some-type',
                },
                11: {
                  id: 11,
                  laoId: mockLaoId,
                  hasBeenRead: false,
                  timestamp: 20,
                  title: 'some title',
                  type: 'some-type',
                },
              },
              nextId: 12,
            },
          },
        } as NotificationReducerState,
      }),
    ).toEqual(2);
  });
});

describe('selectAllNotifications', () => {
  it('returns all notifications', () => {
    const n0 = {
      id: 0,
      laoId: mockLaoId,
      hasBeenRead: true,
      timestamp: 20,
      title: 'some title',
      type: 'some-type',
    } as NotificationState;
    const n1 = {
      id: 1,
      laoId: mockLaoId,
      hasBeenRead: false,
      timestamp: 20,
      title: 'some title',
      type: 'some-type',
    } as NotificationState;
    const n3 = {
      id: 3,
      laoId: mockLaoId,
      hasBeenRead: true,
      timestamp: 20,
      title: 'some title',
      type: 'some-type',
    } as NotificationState;
    const n11 = {
      id: 11,
      laoId: mockLaoId,
      hasBeenRead: false,
      timestamp: 20,
      title: 'some title',
      type: 'some-type',
    } as NotificationState;

    expect(
      makeAllNotificationsSelector(mockLaoId)({
        [NOTIFICATION_REDUCER_PATH]: {
          byLaoId: {
            [mockLaoId]: {
              allIds: [0, 1, 3, 11],
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
    ).toEqual([n0, n1, n3, n11]);
  });
});

describe('getNotification', () => {
  it('returns the correct notification', () => {
    const n0 = {
      id: 0,
      laoId: mockLaoId,
      hasBeenRead: true,
      timestamp: 20,
      title: 'some title',
      type: 'some-type',
    } as NotificationState;
    const n1 = {
      id: 1,
      laoId: mockLaoId,
      hasBeenRead: false,
      timestamp: 20,
      title: 'some title',
      type: 'some-type',
    } as NotificationState;
    const n3 = {
      id: 3,
      laoId: mockLaoId,
      hasBeenRead: true,
      timestamp: 20,
      title: 'some title',
      type: 'some-type',
    } as NotificationState;
    const n11 = {
      id: 11,
      laoId: mockLaoId,
      hasBeenRead: false,
      timestamp: 20,
      title: 'some title',
      type: 'some-type',
    } as NotificationState;

    expect(
      getNotification(mockLaoId, 3, {
        [NOTIFICATION_REDUCER_PATH]: {
          byLaoId: {
            [mockLaoId]: {
              allIds: [0, 1, 3, 11],
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
    const n0 = {
      id: 0,
      laoId: mockLaoId,
      hasBeenRead: true,
      timestamp: 20,
      title: 'some title',
      type: 'some-type',
    } as NotificationState;
    const n1 = {
      id: 1,
      laoId: mockLaoId,
      hasBeenRead: false,
      timestamp: 20,
      title: 'some title',
      type: 'some-type',
    } as NotificationState;
    const n3 = {
      id: 3,
      laoId: mockLaoId,
      hasBeenRead: true,
      timestamp: 20,
      title: 'some title',
      type: 'some-type',
    } as NotificationState;
    const n11 = {
      id: 11,
      laoId: mockLaoId,
      hasBeenRead: false,
      timestamp: 20,
      title: 'some title',
      type: 'some-type',
    } as NotificationState;

    expect(
      getNotification(mockLaoId, 5, {
        [NOTIFICATION_REDUCER_PATH]: {
          byLaoId: {
            [mockLaoId]: {
              allIds: [0, 1, 3, 11],
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
