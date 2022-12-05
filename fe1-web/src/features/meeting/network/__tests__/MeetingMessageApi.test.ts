import 'jest-extended';
import '__tests__/utils/matchers';

import {
  configureTestFeatures,
  defaultMessageDataFields,
  serializedMockLaoId,
  mockLaoId,
} from '__tests__/utils';
import { ActionType, MessageData, ObjectType } from 'core/network/jsonrpc/messages';
import { publish as mockPublish } from 'core/network/JsonRpcApi';
import { Hash, Timestamp } from 'core/objects';

import * as msApi from '../MeetingMessageApi';
import { CreateMeeting } from '../messages';

jest.mock('core/network/JsonRpcApi');
const publishMock = mockPublish as jest.Mock;

const mockEventName = 'myMeeting';
const mockLocation = 'location';
const mockStartTime = new Timestamp(1735685990);
const mockEndTime = new Timestamp(1735686000);

const checkDataCreateMeeting = (obj: MessageData) => {
  expect(obj.object).toBe(ObjectType.MEETING);
  expect(obj.action).toBe(ActionType.CREATE);

  const data: CreateMeeting = obj as CreateMeeting;
  expect(data).toBeObject();
  const expectedMinFields = [...defaultMessageDataFields, 'id', 'name', 'creation', 'start'];
  expect(data).toContainKeys(expectedMinFields);
  expect(data.id).toBeBase64Url();
  expect(data.name).toBeString();
  expect(data.name).toBe(mockEventName);
  expect(data.creation).toBeNumberObject();
  expect(data.creation.valueOf()).toBeGreaterThan(0);

  if ('location' in data) {
    expect(data.location).toBeString();
    expect(data.location).toBe(mockLocation);
  }

  expect(data.start).toBeNumberObject();
  expect(data.start.valueOf()).toBeGreaterThan(0);
  expect(data.start.valueOf()).toBeGreaterThanOrEqual(data.creation.valueOf());

  if ('end' in data) {
    expect(data.end).toBeNumberObject();
    // @ts-ignore
    expect(data.end.valueOf()).toBeGreaterThan(0);
    // @ts-ignore
    expect(data.end.valueOf() + 1).toBeGreaterThan(data.start.valueOf());
  }

  if ('extra' in data) {
    expect(data.extra).toBeObject();
  }

  // check id
  const expected = Hash.fromArray('M', mockLaoId, data.creation, data.name);
  expect(data.id).toEqual(expected);
};

beforeAll(configureTestFeatures);

beforeEach(() => {
  publishMock.mockClear();
});

describe('MessageApi', () => {
  it('should create the correct request for requestCreateMeeting without extra', async () => {
    await msApi.requestCreateMeeting(
      mockLaoId,
      mockEventName,
      mockStartTime,
      mockLocation,
      mockEndTime,
    );

    expect(publishMock).toBeCalledTimes(1);
    const [channel, msgData] = publishMock.mock.calls[0];
    expect(channel).toBe(`/root/${serializedMockLaoId}`);
    checkDataCreateMeeting(msgData);
  });

  it('should create the correct request for requestCreateMeeting with extra', async () => {
    const mockExtra = { numberParticipants: 12, minAge: 18 };
    await msApi.requestCreateMeeting(
      mockLaoId,
      mockEventName,
      mockStartTime,
      mockLocation,
      mockEndTime,
      mockExtra,
    );

    expect(publishMock).toBeCalledTimes(1);
    const [channel, msgData] = publishMock.mock.calls[0];
    expect(channel).toBe(`/root/${serializedMockLaoId}`);
    checkDataCreateMeeting(msgData);
  });
});
