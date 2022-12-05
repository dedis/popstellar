import { mockChannel, mockKeyPair, mockLao, mockLaoId } from '__tests__/utils';
import { ActionType, ObjectType, ProcessableMessage } from 'core/network/jsonrpc/messages';
import { Base64UrlData, EventTags, Hash, Signature, Timestamp } from 'core/objects';
import { Meeting } from 'features/meeting/objects';

import { handleMeetingCreateMessage, handleMeetingStateMessage } from '../MeetingHandler';
import { CreateMeeting, StateMeeting } from '../messages';

const TIMESTAMP = new Timestamp(1609455600); // 1st january 2021

const mockMessageData = {
  receivedAt: TIMESTAMP,
  receivedFrom: 'some address',
  laoId: mockLaoId,
  data: Base64UrlData.encode('some data'),
  sender: mockKeyPair.publicKey,
  signature: Base64UrlData.encode('some data') as Signature,
  channel: mockChannel,
  message_id: new Hash('some string'),
  witness_signatures: [],
};

const mockAddEvent = jest.fn();
const mockUpdateEvent = jest.fn();

const mockMeetingName = 'a meeting';
const mockMeetingId = Hash.fromStringArray(
  EventTags.MEETING,
  mockLaoId,
  TIMESTAMP.toString(),
  mockMeetingName,
);

const mockMeeting = new Meeting({
  id: mockMeetingId,
  name: mockMeetingName,
  location: 'some loc',
  creation: TIMESTAMP,
  start: TIMESTAMP,
  end: undefined,
  extra: {},
});

const mockNewMeeting = new Meeting({
  id: mockMeetingId,
  name: mockMeetingName,
  location: 'some other loc',
  creation: TIMESTAMP,
  start: TIMESTAMP,
  end: undefined,
  extra: {},
});

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

    it('should return false if the message is not received on a lao channel', () => {
      expect(
        handleMeetingCreateMessage(mockAddEvent)({
          ...mockMessageData,
          laoId: undefined,
          messageData: new CreateMeeting(
            {
              id: mockMeeting.id,
              name: mockMeeting.name,
              creation: mockMeeting.creation,
              location: mockMeeting.location,
              start: mockMeeting.start,
              end: mockMeeting.end,
              extra: mockMeeting.extra,
            },
            mockLaoId,
          ),
        } as ProcessableMessage),
      ).toBeFalse();
    });

    it('should dispatch the correct action and return true on success', () => {
      expect(
        handleMeetingCreateMessage(mockAddEvent)({
          ...mockMessageData,
          messageData: new CreateMeeting(
            {
              id: mockMeeting.id,
              name: mockMeeting.name,
              creation: mockMeeting.creation,
              location: mockMeeting.location,
              start: mockMeeting.start,
              end: mockMeeting.end,
              extra: mockMeeting.extra,
            },
            mockLaoId,
          ),
        } as ProcessableMessage),
      ).toBeTrue();

      expect(mockAddEvent).toHaveBeenCalledTimes(1);
      expect(mockAddEvent).toHaveBeenCalledWith(mockLaoId, mockMeeting);
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

    it('should return false if the message is not received on a lao channel', () => {
      expect(
        handleMeetingStateMessage(
          () => mockLao,
          jest.fn(() => mockMeeting),
          mockUpdateEvent,
        )({
          ...mockMessageData,
          laoId: undefined,
          messageData: new StateMeeting({
            id: mockNewMeeting.id,
            name: mockNewMeeting.name,
            creation: mockNewMeeting.creation,
            location: mockNewMeeting.location,
            start: mockNewMeeting.start,
            end: mockNewMeeting.end,
            extra: mockNewMeeting.extra,
            last_modified: TIMESTAMP,
            modification_id: mockNewMeeting.id,
            modification_signatures: [],
          }),
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
      const mockGetEventById = jest.fn(() => mockMeeting);

      expect(
        handleMeetingStateMessage(
          () => mockLao,
          mockGetEventById,
          mockUpdateEvent,
        )({
          ...mockMessageData,
          messageData: new StateMeeting({
            id: mockNewMeeting.id,
            name: mockNewMeeting.name,
            creation: mockNewMeeting.creation,
            location: mockNewMeeting.location,
            start: mockNewMeeting.start,
            end: mockNewMeeting.end,
            extra: mockNewMeeting.extra,
            last_modified: TIMESTAMP,
            modification_id: mockNewMeeting.id,
            modification_signatures: [],
          }),
        } as ProcessableMessage),
      ).toBeTrue();

      expect(mockUpdateEvent).toHaveBeenCalledTimes(1);
      expect(mockUpdateEvent).toHaveBeenCalledWith(mockNewMeeting);
    });
  });
});
