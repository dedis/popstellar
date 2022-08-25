/**
 * This error is disabled since reducers use the createSlice function, which requires the user to
 * param-reassign. Please do not disable other errors.
 */
/* eslint-disable no-param-reassign */
import { createSelector, createSlice, Draft, PayloadAction } from '@reduxjs/toolkit';

import { Hash } from 'core/objects';

import { Meeting, MeetingState } from '../objects';

/**
 * Reducer & associated function implementation to store all known meetings
 */

export interface MeetingReducerState {
  byId: Record<string, MeetingState>;
  allIds: string[];
}

const initialState: MeetingReducerState = {
  byId: {},
  allIds: [],
};

export const MEETING_REDUCER_PATH = 'meeting';

const meetingSlice = createSlice({
  name: MEETING_REDUCER_PATH,
  initialState,
  reducers: {
    addMeeting: (state: Draft<MeetingReducerState>, action: PayloadAction<MeetingState>) => {
      const newMeeting = action.payload;

      if (newMeeting.id in state.byId) {
        throw new Error(`Tried to add meeting with id ${newMeeting.id} but it already exists`);
      }

      state.allIds.push(newMeeting.id);
      state.byId[newMeeting.id] = newMeeting;
    },

    updateMeeting: (state: Draft<MeetingReducerState>, action: PayloadAction<MeetingState>) => {
      const updatedMeeting = action.payload;

      if (!(updatedMeeting.id in state.byId)) {
        throw new Error(`Tried to update inexistent meeting with id ${updatedMeeting.id}`);
      }

      state.byId[updatedMeeting.id] = updatedMeeting;
    },

    removeMeeting: (state, action: PayloadAction<Hash | string>) => {
      const meetingId = action.payload.valueOf();

      if (!(meetingId in state.byId)) {
        throw new Error(`Tried to delete inexistent meeting with id ${meetingId}`);
      }

      delete state.byId[meetingId];
      state.allIds = state.allIds.filter((id) => id !== meetingId);
    },
  },
});

export const { addMeeting, updateMeeting, removeMeeting } = meetingSlice.actions;

export const getMeetingState = (state: any): MeetingReducerState => state[MEETING_REDUCER_PATH];

export const meetingReduce = meetingSlice.reducer;

export default {
  [MEETING_REDUCER_PATH]: meetingSlice.reducer,
};

const sGetMeetingState = (state: any) => getMeetingState(state).byId;

/**
 * Creates a selector that retrieves an meeting by its id
 * @param meetingId The if of the meeting / event to retrieve
 * @returns The selector
 */
export const makeMeetingSelector = (meetingId: Hash | string) => {
  const meetingIdString = meetingId.valueOf();

  return createSelector(
    // First input: map from ids to meetings
    sGetMeetingState,
    // Selector: returns the selected meeting
    (meetingById: Record<string, MeetingState>): Meeting | undefined => {
      if (!(meetingIdString in meetingById)) {
        return undefined;
      }

      return Meeting.fromState(meetingById[meetingIdString]);
    },
  );
};

/**
 * Retrieves an meeting by its id from the redux store
 * @remark This function does not memoize its result, use 'makeMeetingSelector' in react components
 * @param meetingId The if of the meeting / event to retrieve
 * @param state The redux state
 * @returns The constructed meeting or undefined if the id is not found
 */
export const getMeetingById = (meetingId: Hash | string, state: unknown) => {
  const meetingIdString = meetingId.valueOf();

  const meetingById = getMeetingState(state).byId;

  if (!(meetingIdString in meetingById)) {
    return undefined;
  }

  return Meeting.fromState(meetingById[meetingIdString]);
};
