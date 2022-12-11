import { AnyAction } from 'redux';

import { mockMeeting, mockMeeting2 } from '../../__tests__/utils';
import { Meeting, MeetingState } from '../../objects';
import {
  addMeeting,
  meetingReduce,
  MeetingReducerState,
  MEETING_REDUCER_PATH,
  makeMeetingSelector,
  removeMeeting,
  updateMeeting,
} from '../MeetingReducer';

const mockMeetingState: MeetingState = mockMeeting.toState();
const mockMeetingState2: MeetingState = mockMeeting2.toState();

describe('MeetingReducer', () => {
  it('returns a valid initial state', () => {
    expect(meetingReduce(undefined, {} as AnyAction)).toEqual({
      byId: {},
      allIds: [],
    } as MeetingReducerState);
  });

  describe('addMeeting', () => {
    it('adds new meetings to the state', () => {
      expect(
        meetingReduce(
          {
            byId: {},
            allIds: [],
          } as MeetingReducerState,
          addMeeting(mockMeetingState),
        ),
      ).toEqual({
        byId: {
          [mockMeetingState.id]: mockMeetingState,
        },
        allIds: [mockMeetingState.id],
      } as MeetingReducerState);
    });

    it('throws an error if the store already contains an meeting with the same id', () => {
      expect(() =>
        meetingReduce(
          {
            byId: {
              [mockMeetingState.id]: mockMeetingState,
            },
            allIds: [mockMeetingState.id],
          } as MeetingReducerState,
          addMeeting(mockMeetingState),
        ),
      ).toThrow();
    });
  });

  describe('updateMeeting', () => {
    it('updates meetings in the state', () => {
      expect(
        meetingReduce(
          {
            byId: {
              [mockMeetingState.id]: mockMeetingState,
            },
            allIds: [mockMeetingState.id],
          } as MeetingReducerState,
          updateMeeting(mockMeetingState2),
        ),
      ).toEqual({
        byId: {
          [mockMeetingState.id]: mockMeetingState2,
        },
        allIds: [mockMeetingState.id],
      } as MeetingReducerState);
    });

    it('throws an error when trying to update an inexistent meeting', () => {
      expect(() =>
        meetingReduce(
          {
            byId: {},
            allIds: [],
          } as MeetingReducerState,
          updateMeeting(mockMeetingState),
        ),
      ).toThrow();
    });
  });

  describe('removeMeeting', () => {
    it('removes meetings from the state', () => {
      expect(
        meetingReduce(
          {
            byId: {
              [mockMeetingState.id]: mockMeetingState,
            },
            allIds: [mockMeetingState.id],
          } as MeetingReducerState,
          removeMeeting(mockMeeting.id),
        ),
      ).toEqual({
        byId: {},
        allIds: [],
      } as MeetingReducerState);
    });

    it('throws an error when trying to remove an inexistent meeting', () => {
      expect(() =>
        meetingReduce(
          {
            byId: {},
            allIds: [],
          } as MeetingReducerState,
          removeMeeting(mockMeeting.id),
        ),
      ).toThrow();
    });
  });

  describe('makeMeetingSelector', () => {
    it('returns the constructed meeting', () => {
      const meeting = makeMeetingSelector(mockMeeting.id)({
        [MEETING_REDUCER_PATH]: {
          byId: { [mockMeetingState.id]: mockMeetingState },
          allIds: [mockMeetingState.id],
        } as MeetingReducerState,
      });
      expect(meeting).toBeInstanceOf(Meeting);
      expect(meeting?.toState()).toEqual(mockMeetingState);
    });

    it('returns undefined if the id of the meeting is not in the store', () => {
      const meeting = makeMeetingSelector(mockMeeting.id)({
        [MEETING_REDUCER_PATH]: {
          byId: {},
          allIds: [],
        } as MeetingReducerState,
      });
      expect(meeting).toBeUndefined();
    });
  });
});
