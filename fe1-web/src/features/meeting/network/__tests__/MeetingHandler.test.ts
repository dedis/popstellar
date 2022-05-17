import { mockChannel, mockKeyPair, mockLao, mockLaoId, mockLaoIdHash } from '__tests__/utils';
import { ActionType, ObjectType, ProcessableMessage } from 'core/network/jsonrpc/messages';
import { Base64UrlData, EventTags, Hash, Signature, Timestamp } from 'core/objects';
import { dispatch } from 'core/redux';
import { MeetingFeature } from 'features/meeting/interface';
import { Meeting } from 'features/meeting/objects';

import { handleMeetingCreateMessage, handleMeetingStateMessage } from '../MeetingHandler';
import { CreateMeeting, StateMeeting } from '../messages';

const TIMESTAMP = new Timestamp(1609455600); // 1st january 2021

const mockMessageData = {
  receivedAt: TIMESTAMP,
  receivedFrom: 'some address',
  laoId: mockLaoIdHash,
  data: Base64UrlData.encode('some data'),
  sender: mockKeyPair.publicKey,
  signature: Base64UrlData.encode('some data') as Signature,
  channel: mockChannel,
  message_id: Hash.fromString('some string'),
  witness_signatures: [],
};

const mockAddEvent = jest.fn((laoId: string | Hash, meeting: MeetingFeature.EventState) => ({
  type: 'add-event',
  laoId: laoId.valueOf(),
  meeting,
}));
const mockUpdateEvent = jest.fn((laoId: string | Hash, meeting: MeetingFeature.EventState) => ({
  type: 'update-event',
  laoId: laoId.valueOf(),
  meeting,
}));

jest.mock('core/redux', () => {
  const actualModule = jest.requireActual('core/redux');
  return {
    ...actualModule,
    dispatch: jest.fn(() => {}),
  };
});

beforeEach(() => {
  jest.resetAllMocks();
});

describe('MeetingHandler', () => {
  describe('handleMeetingCreateMessage', () => {
    it('should return false if the object type is wrong', () => {
      expect(
        handleMeetingCreateMessage(mockAddEvent)({
          ...mockMessageData,
          messageData: {
            object: ObjectType.CHIRP,
            action: ActionType.CREATE,
          },
        } as ProcessableMessage),
      ).toBeFalse();
    });

    it('should return false if the action type is wrong', () => {
      expect(
        handleMeetingCreateMessage(mockAddEvent)({
          ...mockMessageData,
          messageData: {
            object: ObjectType.MEETING,
            action: ActionType.ADD,
          },
        } as ProcessableMessage),
      ).toBeFalse();
    });

    it('should dispatch the correct action and return true on success', () => {
      const meetingName = 'a meeting';
      const meetingId = Hash.fromStringArray(
        EventTags.MEETING,
        mockLaoId.toString(),
        TIMESTAMP.toString(),
        meetingName,
      );

      const meeting = new Meeting({
        id: meetingId,
        name: meetingName,
        location: 'some loc',
        creation: TIMESTAMP,
        start: TIMESTAMP,
        end: undefined,
        extra: {},
      });

      expect(
        handleMeetingCreateMessage(mockAddEvent)({
          ...mockMessageData,
          messageData: new CreateMeeting({
            id: meeting.id,
            name: meeting.name,
            creation: meeting.creation,
            location: meeting.location,
            start: meeting.start,
            end: meeting.end,
            extra: meeting.extra,
          }),
        } as ProcessableMessage),
      ).toBeTrue();

      expect(dispatch).toHaveBeenCalledTimes(1);
      expect(dispatch).toHaveBeenCalledWith(mockAddEvent(mockLaoId, meeting.toState()));
    });
  });

  describe('handleMeetingStateMessage', () => {
    it('should return false if the object type is wrong', () => {
      expect(
        handleMeetingStateMessage(
          jest.fn(),
          jest.fn(),
          jest.fn(),
        )({
          ...mockMessageData,
          messageData: {
            object: ObjectType.CHIRP,
            action: ActionType.STATE,
          },
        } as ProcessableMessage),
      ).toBeFalse();
    });

    it('should return false if the action type is wrong', () => {
      expect(
        handleMeetingStateMessage(
          jest.fn(),
          jest.fn(),
          jest.fn(),
        )({
          ...mockMessageData,
          messageData: {
            object: ObjectType.MEETING,
            action: ActionType.ADD,
          },
        } as ProcessableMessage),
      ).toBeFalse();
    });

    it('should return false if there is no active lao', () => {
      expect(
        handleMeetingStateMessage(
          jest.fn(),
          jest.fn(),
          jest.fn(),
        )({
          ...mockMessageData,
          messageData: {
            object: ObjectType.MEETING,
            action: ActionType.STATE,
          },
        } as ProcessableMessage),
      ).toBeFalse();
    });

    it('should dispatch the correct action and return true on success', () => {
      const meetingName = 'a meeting';
      const meetingId = Hash.fromStringArray(
        EventTags.MEETING,
        mockLaoId.toString(),
        TIMESTAMP.toString(),
        meetingName,
      );

      const oldMeeting = new Meeting({
        id: meetingId,
        name: meetingName,
        location: 'some loc',
        creation: TIMESTAMP,
        start: TIMESTAMP,
        end: undefined,
        extra: {},
      });

      const newMeeting = new Meeting({
        id: meetingId,
        name: meetingName,
        location: 'some other loc',
        creation: TIMESTAMP,
        start: TIMESTAMP,
        end: undefined,
        extra: {},
      });

      const mockGetEventById = jest.fn(() => oldMeeting);

      expect(
        handleMeetingStateMessage(
          () => mockLao,
          mockGetEventById,
          mockUpdateEvent,
        )({
          ...mockMessageData,
          messageData: new StateMeeting({
            id: newMeeting.id,
            name: newMeeting.name,
            creation: newMeeting.creation,
            location: newMeeting.location,
            start: newMeeting.start,
            end: newMeeting.end,
            extra: newMeeting.extra,
            last_modified: TIMESTAMP,
            modification_id: newMeeting.id,
            modification_signatures: [],
          }),
        } as ProcessableMessage),
      ).toBeTrue();

      expect(dispatch).toHaveBeenCalledTimes(1);
      expect(dispatch).toHaveBeenCalledWith(mockUpdateEvent(mockLaoId, newMeeting.toState()));
    });
  });
});
