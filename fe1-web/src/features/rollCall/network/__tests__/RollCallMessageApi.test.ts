import 'jest-extended';
import '__tests__/utils/matchers';

import {
  configureTestFeatures,
  defaultMessageDataFields,
  mockLaoId,
  mockLaoIdHash,
} from '__tests__/utils';
import { ActionType, MessageData, ObjectType } from 'core/network/jsonrpc/messages';
import { publish as mockPublish } from 'core/network/JsonRpcApi';
import { Hash, PublicKey, Timestamp } from 'core/objects';

import { CloseRollCall, CreateRollCall, OpenRollCall, ReopenRollCall } from '../messages';
import * as msApi from '../RollCallMessageApi';

jest.mock('core/network/JsonRpcApi');
const publishMock = mockPublish as jest.Mock;

const mockEventName = 'myRollCall';
const mockLocation = 'location';
const mockStartTime = new Timestamp(1735685990);
const mockEndTime = new Timestamp(1735686000);
const mockRollCallId = Hash.fromString('my-roll-call');

const checkDataCreateRollCall = (obj: MessageData) => {
  expect(obj.object).toBe(ObjectType.ROLL_CALL);
  expect(obj.action).toBe(ActionType.CREATE);

  const data: CreateRollCall = obj as CreateRollCall;
  expect(data).toBeObject();
  expect(data).toContainKeys([
    ...defaultMessageDataFields,
    'id',
    'name',
    'creation',
    'location',
    'proposed_start',
    'proposed_end',
  ]);
  expect(data.id).toBeBase64Url();
  expect(data.name).toBeString();
  expect(data.name).toBe('myRollCall');
  expect(data.creation).toBeNumberObject();
  expect(data.creation.valueOf()).toBeGreaterThan(0);
  expect(data.proposed_start).toBeNumberObject();
  expect(data.proposed_start.valueOf()).toBeGreaterThan(0);
  expect(data.proposed_start.valueOf() + 1).toBeGreaterThan(data.creation.valueOf());
  expect(data.proposed_end).toBeNumberObject();
  expect(data.proposed_end.valueOf()).toBeGreaterThan(0);
  expect(data.proposed_end.valueOf() + 1).toBeGreaterThan(data.creation.valueOf());
  expect(data.location).toBeString();
  expect(data.location).toBe('location');

  if ('description' in data) {
    expect(data.description).toBeString();
  }

  // Check id
  const expected = Hash.fromStringArray(
    'R',
    mockLaoId.toString(),
    data.creation.toString(),
    data.name,
  );
  expect(data.id).toEqual(expected);
};

const checkDataOpenRollCall = (obj: MessageData) => {
  expect(obj.object).toBe(ObjectType.ROLL_CALL);
  expect(obj.action).toBe(ActionType.OPEN);

  const data: OpenRollCall = obj as OpenRollCall;
  expect(data).toBeObject();
  expect(data).toContainKeys([...defaultMessageDataFields, 'update_id', 'opens', 'opened_at']);
  expect(data.update_id).toBeBase64Url();
  expect(data.opens).toBeBase64Url();
  expect(data.opened_at).toBeNumberObject();
  expect(data.opened_at.valueOf()).toBeGreaterThan(0);

  // Check id
  const expected = Hash.fromStringArray(
    'R',
    mockLaoId.toString(),
    data.opens.toString(),
    data.opened_at.toString(),
  );
  expect(data.update_id).toEqual(expected);
};

const checkDataReopenRollCall = (obj: MessageData) => {
  expect(obj.object).toBe(ObjectType.ROLL_CALL);
  expect(obj.action).toBe(ActionType.REOPEN);

  const data: ReopenRollCall = obj as ReopenRollCall;
  expect(data).toContainKeys([...defaultMessageDataFields, 'update_id', 'opens', 'opened_at']);
  expect(data.update_id).toBeBase64Url();
  expect(data.opens).toBeBase64Url();
  expect(data.opened_at).toBeNumberObject();
  expect(data.opened_at.valueOf()).toBeGreaterThan(0);

  // check id
  const expected = Hash.fromStringArray(
    'R',
    mockLaoId.toString(),
    data.opens.toString(),
    data.opened_at.toString(),
  );
  expect(data.update_id).toEqual(expected);
};

const checkDataCloseRollCall = (obj: MessageData) => {
  expect(obj.object).toBe(ObjectType.ROLL_CALL);
  expect(obj.action).toBe(ActionType.CLOSE);

  const data: CloseRollCall = obj as CloseRollCall;
  expect(data).toBeObject();
  expect(data).toContainKeys([
    ...defaultMessageDataFields,
    'update_id',
    'closes',
    'closed_at',
    'attendees',
  ]);
  expect(data.update_id).toBeBase64Url();
  expect(data.closed_at).toBeNumberObject();
  expect(data.closed_at.valueOf()).toBeGreaterThan(0);
  expect(data.attendees).toBeBase64UrlArray();
  expect(data.attendees).toBeDistinctArray();

  // check id
  const expected = Hash.fromStringArray(
    'R',
    mockLaoId,
    data.closes.toString(),
    data.closed_at.toString(),
  );
  expect(data.update_id).toEqual(expected);
};

beforeAll(() => {
  configureTestFeatures();
});

beforeEach(() => {
  publishMock.mockClear();
});

describe('MessageApi', () => {
  it('should create the correct request for requestCreateRollCall without description', async () => {
    await msApi.requestCreateRollCall(
      mockLaoIdHash,
      mockEventName,
      mockLocation,
      mockStartTime,
      mockEndTime,
    );
  });

  it('should create the correct request for requestCreateRollCall with description', async () => {
    const mockDescription = 'random description';
    await msApi.requestCreateRollCall(
      mockLaoIdHash,
      mockEventName,
      mockLocation,
      mockStartTime,
      mockEndTime,
      mockDescription,
    );

    expect(publishMock).toBeCalledTimes(1);
    const [channel, msgData] = publishMock.mock.calls[0];
    expect(channel).toBe(`/root/${mockLaoId}`);
    checkDataCreateRollCall(msgData);
  });

  it('should create the correct request for requestOpenRollCall without start time', async () => {
    await msApi.requestOpenRollCall(mockLaoIdHash, mockRollCallId);

    expect(publishMock).toBeCalledTimes(1);
    const [channel, msgData] = publishMock.mock.calls[0];
    expect(channel).toBe(`/root/${mockLaoId}`);
    checkDataOpenRollCall(msgData);
  });

  it('should create the correct request for requestOpenRollCall with start time', async () => {
    await msApi.requestOpenRollCall(mockLaoIdHash, mockRollCallId, mockStartTime);

    expect(publishMock).toBeCalledTimes(1);
    const [channel, msgData] = publishMock.mock.calls[0];
    expect(channel).toBe(`/root/${mockLaoId}`);
    checkDataOpenRollCall(msgData);
  });

  it('should create the correct request for requestReopenRollCall without start time', async () => {
    await msApi.requestReopenRollCall(mockLaoIdHash, mockRollCallId);

    expect(publishMock).toBeCalledTimes(1);
    const [channel, msgData] = publishMock.mock.calls[0];
    expect(channel).toBe(`/root/${mockLaoId}`);
    checkDataReopenRollCall(msgData);
  });

  it('should create the correct request for requestReopenRollCall with start time', async () => {
    await msApi.requestReopenRollCall(mockLaoIdHash, mockRollCallId, mockStartTime);

    expect(publishMock).toBeCalledTimes(1);
    const [channel, msgData] = publishMock.mock.calls[0];
    expect(channel).toBe(`/root/${mockLaoId}`);
    checkDataReopenRollCall(msgData);
  });

  it('should create the correct request for requestCloseRollCall without attendees', async () => {
    await msApi.requestCloseRollCall(mockLaoIdHash, mockRollCallId, []);

    expect(publishMock).toBeCalledTimes(1);
    const [channel, msgData] = publishMock.mock.calls[0];
    expect(channel).toBe(`/root/${mockLaoId}`);
    checkDataCloseRollCall(msgData);
  });

  it('should create the correct request for requestCloseRollCall with attendees', async () => {
    const attendeePks: PublicKey[] = [
      'NdCY6Ga7yVK5rZay3S4xGjlz2jHZQrg2gw7fyI6tfuo=',
      'BEW-uVz_NG_prXFuaKrI9Ae0EbBLGWehLQ8aLZFWY4w=',
    ].map((a) => new PublicKey(a));

    await msApi.requestCloseRollCall(mockLaoIdHash, mockRollCallId, attendeePks);

    expect(publishMock).toBeCalledTimes(1);
    const [channel, msgData] = publishMock.mock.calls[0];
    expect(channel).toBe(`/root/${mockLaoId}`);
    expect((msgData as CloseRollCall).attendees).toEqual(attendeePks);
    checkDataCloseRollCall(msgData);
  });

  it('should create the correct request for requestCloseRollCall with end time', async () => {
    await msApi.requestCloseRollCall(mockLaoIdHash, mockRollCallId, [], mockEndTime);

    expect(publishMock).toBeCalledTimes(1);
    const [channel, msgData] = publishMock.mock.calls[0];
    expect(channel).toBe(`/root/${mockLaoId}`);
    expect((msgData as CloseRollCall).closed_at).toEqual(mockEndTime);
    checkDataCloseRollCall(msgData);
  });
});
