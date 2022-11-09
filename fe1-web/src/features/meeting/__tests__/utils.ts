import { mockLaoIdHash } from '__tests__/utils';
import { Timestamp } from 'core/objects';

import { CreateMeeting } from '../network/messages';
import { Meeting, MeetingState } from '../objects';

const TIMESTAMP = new Timestamp(1610000000); // 7th of january 2021

const mockMeetingName = 'a meeting';
const mockMeetingLocation = 'on earth';
const mockMeetingCreation = TIMESTAMP;
const mockMeetingLastModified = TIMESTAMP;
const mockMeetingStart = TIMESTAMP;
const mockMeetingEnd = TIMESTAMP;
const mockMeetingExtra = {
  some: 'data',
};

export const mockMeeting: Meeting = new Meeting({
  id: CreateMeeting.computeMeetingId(mockLaoIdHash, mockMeetingCreation, mockMeetingName),
  name: mockMeetingName,
  location: mockMeetingLocation,
  creation: mockMeetingCreation,
  lastModified: mockMeetingLastModified,
  start: mockMeetingStart,
  end: mockMeetingEnd,
  extra: mockMeetingExtra,
});

export const mockMeetingState: MeetingState = mockMeeting.toState();

// a meeting with the same id as mockMeeting but a different location
const mockMeetingLocation2 = 'on pluto';

export const mockMeeting2: Meeting = new Meeting({
  id: CreateMeeting.computeMeetingId(mockLaoIdHash, mockMeetingCreation, mockMeetingName),
  name: mockMeetingName,
  location: mockMeetingLocation2,
  creation: mockMeetingCreation,
  lastModified: mockMeetingLastModified,
  start: mockMeetingStart,
  end: mockMeetingEnd,
  extra: mockMeetingExtra,
});

export const mockMeetingState2: MeetingState = mockMeeting2.toState();
