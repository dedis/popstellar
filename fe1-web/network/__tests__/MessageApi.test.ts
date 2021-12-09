import 'jest-extended';
import '__tests__/utils/matchers';

import 'store/Storage';
import { KeyPairStore, OpenedLaoStore } from 'store';
import {
  ActionType,
  CloseRollCall,
  CreateLao,
  CreateMeeting,
  CreateRollCall,
  MessageData,
  ObjectType,
  OpenRollCall,
  ReopenRollCall,
  StateLao,
  UpdateLao,
  WitnessMessage,
} from 'model/network/method/message/data';
import {
  Hash, Lao, Timestamp, KeyPair, Base64UrlData, PublicKey, PopToken,
} from 'model/objects';

// @ts-ignore
import testKeyPair from 'test_data/keypair.json';

import * as msApi from '../MessageApi';
import { publish } from '../JsonRpcApi';

jest.mock('network/JsonRpcApi.ts', () => ({
  publish: jest.fn(() => Promise.resolve()),
}));
const publishMock = publish as jest.MockedFunction<typeof publish>;

const mockPopToken = PopToken.fromState({
  publicKey: '123',
  privateKey: '456',
});
jest.mock('model/objects/wallet/Token.ts', () => ({
  generateToken: jest.fn(async () => mockPopToken),
}));

const mockEventName = 'Random Name';
const mockLocation = 'EPFL';
const mockCreationTime = new Timestamp(1609455600);
const mockStartTime = new Timestamp(1735685990);
const mockEndTime = new Timestamp(1735686000);
const mockRollCallId = Hash.fromString('my-roll-call');

const defaultDataFields = ['object', 'action'];

function checkDataCreateLao(obj: MessageData) {
  expect(obj.object).toBe(ObjectType.LAO);
  expect(obj.action).toBe(ActionType.CREATE);

  const data: CreateLao = obj as CreateLao;

  expect(data).toBeObject();
  expect(data).toContainKeys([...defaultDataFields, 'id', 'name', 'creation',
    'organizer', 'witnesses']);

  expect(data.id).toBeBase64Url();

  expect(data.name).toBeString();
  expect(data.name).toBe(mockEventName);

  expect(data.creation).toBeNumberObject();
  expect(data.creation.valueOf()).toBeGreaterThan(0);

  expect(data.organizer).toBeBase64Url();
  expect(data.organizer).toBeJsonEqual(KeyPairStore.getPublicKey());

  expect(data.witnesses).toBeBase64UrlArray();
  expect(data.witnesses).toBeDistinctArray();

  // check id
  const expected: Hash = Hash.fromStringArray(
    data.organizer.toString(), data.creation.toString(), data.name,
  );
  expect(data.id).toBeJsonEqual(expected);
}

function checkDataUpdateLao(obj: MessageData) {
  expect(obj.object).toBe(ObjectType.LAO);
  expect(obj.action).toBe(ActionType.UPDATE_PROPERTIES);

  const data: UpdateLao = obj as UpdateLao;

  expect(data).toBeObject();
  expect(data).toContainKeys([...defaultDataFields, 'id', 'name', 'last_modified', 'witnesses']);

  expect(data.id).toBeBase64Url();

  expect(data.name).toBeString();
  expect(data.name).toBe(mockEventName);

  expect(data.last_modified).toBeNumberObject();
  expect(data.last_modified.valueOf()).toBeGreaterThan(0);

  expect(data.witnesses).toBeArray();
  data.witnesses.forEach((wit) => {
    expect(wit).toBeBase64Url();
  });
  expect(data.witnesses).toHaveLength(new Set(data.witnesses).size);

  // check id
  const expected = Hash.fromStringArray(
    OpenedLaoStore.get().organizer.toString(),
    OpenedLaoStore.get().creation.toString(),
    data.name,
  );
  expect(data.id).toBeJsonEqual(expected);
}

function checkDataStateLao(obj: MessageData) {
  expect(obj.object).toBe(ObjectType.LAO);
  expect(obj.action).toBe(ActionType.STATE);

  const data: StateLao = obj as StateLao;

  expect(data).toBeObject();
  expect(data).toContainKeys([...defaultDataFields, 'id', 'name', 'creation',
    'last_modified', 'organizer', 'witnesses', 'modification_id', 'modification_signatures']);

  expect(data.id).toBeBase64Url();

  expect(data.name).toBeString();
  expect(data.name).toBe(OpenedLaoStore.get().name);

  expect(data.creation).toBeNumberObject();
  expect(data.creation.valueOf()).toBeGreaterThan(0);

  expect(data.last_modified).toBeNumberObject();
  expect(data.last_modified.valueOf()).toBeGreaterThan(0);
  expect(data.last_modified.valueOf() + 1).toBeGreaterThan(data.creation.valueOf());

  expect(data.organizer).toBeBase64Url();
  expect(data.organizer).toBeJsonEqual(OpenedLaoStore.get().organizer);

  expect(data.witnesses).toBeBase64UrlArray();
  expect(data.witnesses).toBeDistinctArray();

  expect(data.modification_id).toBeBase64Url();

  expect(data.modification_signatures).toBeKeySignatureArray('witness', 'signature');

  // check id
  const expected = Hash.fromStringArray(
    data.organizer.toString(), OpenedLaoStore.get().creation.toString(), data.name,
  );
  expect(data.id).toBeJsonEqual(expected);
}

function checkDataCreateMeeting(obj: MessageData) {
  expect(obj.object).toBe(ObjectType.MEETING);
  expect(obj.action).toBe(ActionType.CREATE);

  const data: CreateMeeting = obj as CreateMeeting;

  expect(data).toBeObject();
  const expectedMinFields = [...defaultDataFields, 'id', 'name', 'creation', 'start'];
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
  const expected = Hash.fromStringArray('M', OpenedLaoStore.get().id.toString(), OpenedLaoStore.get().creation.toString(), data.name);
  expect(data.id).toEqual(expected);
}

function checkDataWitnessMessage(obj: MessageData) {
  expect(obj.object).toBe(ObjectType.MESSAGE);
  expect(obj.action).toBe(ActionType.WITNESS);

  const data: WitnessMessage = obj as WitnessMessage;

  expect(data).toContainKeys([...defaultDataFields, 'message_id', 'signature']);
  expect(data.message_id).toBeBase64Url();
  expect(data.signature).toBeBase64Url();
}

function checkDataCreateRollCall(obj: MessageData) {
  expect(obj.object).toBe(ObjectType.ROLL_CALL);
  expect(obj.action).toBe(ActionType.CREATE);

  const data: CreateRollCall = obj as CreateRollCall;

  expect(data).toBeObject();
  expect(data).toContainKeys([...defaultDataFields, 'id', 'name', 'creation', 'location', 'proposed_start', 'proposed_end']);

  expect(data.id).toBeBase64Url();

  expect(data.name).toBeString();
  expect(data.name).toBe(mockEventName);

  expect(data.creation).toBeNumberObject();
  expect(data.creation.valueOf()).toBeGreaterThan(0);

  expect(data.proposed_start).toBeNumberObject();
  // @ts-ignore
  expect(data.proposed_start.valueOf()).toBeGreaterThan(0);
  // @ts-ignore
  expect(data.proposed_start.valueOf() + 1).toBeGreaterThan(data.creation.valueOf());

  expect(data.proposed_end).toBeNumberObject();
  // @ts-ignore
  expect(data.proposed_end.valueOf()).toBeGreaterThan(0);
  // @ts-ignore
  expect(data.proposed_end.valueOf() + 1).toBeGreaterThan(data.creation.valueOf());

  expect(data.location).toBeString();
  expect(data.location).toBe(mockLocation);

  if ('description' in data) {
    expect(data.description).toBeString();
  }

  // check id
  const expected = Hash.fromStringArray('R', OpenedLaoStore.get().id.toString(), data.creation.toString(), data.name);
  expect(data.id).toEqual(expected);
}

function checkDataOpenRollCall(obj: MessageData) {
  expect(obj.object).toBe(ObjectType.ROLL_CALL);
  expect(obj.action).toBe(ActionType.OPEN);

  const data: OpenRollCall = obj as OpenRollCall;

  expect(data).toBeObject();
  expect(data).toContainKeys([...defaultDataFields, 'update_id', 'opens', 'opened_at']);

  expect(data.update_id).toBeBase64Url();
  expect(data.opens).toBeBase64Url();

  expect(data.opened_at).toBeNumberObject();
  expect(data.opened_at.valueOf()).toBeGreaterThan(0);

  // check id
  const expected = Hash.fromStringArray('R', OpenedLaoStore.get().id.toString(),
    data.opens.toString(), data.opened_at.toString());
  expect(data.update_id).toEqual(expected);
}

function checkDataReopenRollCall(obj: MessageData): ReopenRollCall {
  expect(obj.object).toBe(ObjectType.ROLL_CALL);
  expect(obj.action).toBe(ActionType.REOPEN);

  const data: ReopenRollCall = obj as ReopenRollCall;

  expect(data).toContainKeys([...defaultDataFields, 'update_id', 'opens', 'opened_at']);

  expect(data.update_id).toBeBase64Url();
  expect(data.opens).toBeBase64Url();
  expect(data.opened_at).toBeNumberObject();
  expect(data.opened_at.valueOf()).toBeGreaterThan(0);

  // check id
  const expected = Hash.fromStringArray('R', OpenedLaoStore.get().id.toString(),
    data.opens.toString(), data.opened_at.toString());
  expect(data.update_id).toEqual(expected);

  return data;
}

function checkDataCloseRollCall(obj: MessageData): CloseRollCall {
  expect(obj.object).toBe(ObjectType.ROLL_CALL);
  expect(obj.action).toBe(ActionType.CLOSE);

  const data: CloseRollCall = obj as CloseRollCall;

  expect(data).toBeObject();
  expect(data).toContainKeys([...defaultDataFields, 'update_id', 'closes', 'closed_at', 'attendees']);

  expect(data.update_id).toBeBase64Url();

  expect(data.closed_at).toBeNumberObject();
  expect(data.closed_at.valueOf()).toBeGreaterThan(0);

  expect(data.attendees).toBeBase64UrlArray();
  expect(data.attendees).toBeDistinctArray();

  // check id
  const expected = Hash.fromStringArray('R', OpenedLaoStore.get().id.toString(),
    data.closes.toString(), data.closed_at.toString());
  expect(data.update_id).toEqual(expected);

  return data;
}

describe('=== WebsocketApi tests ===', () => {
  let dateNowSpy: jest.SpyInstance<number>;
  beforeAll(() => {
    dateNowSpy = jest.spyOn(Date, 'now')
      .mockImplementation(() => mockCreationTime.valueOf() * 1000);

    KeyPairStore.store(KeyPair.fromState({
      publicKey: testKeyPair.publicKey,
      privateKey: testKeyPair.privateKey,
    }));
  });
  afterAll(() => {
    dateNowSpy.mockRestore();
  });

  let sampleLao: Lao;
  beforeEach(() => {
    publishMock.mockClear();

    const org = KeyPairStore.getPublicKey();
    const time = Timestamp.EpochNow();
    const name: string = 'Pop\'s LAO';
    sampleLao = new Lao({
      name,
      id: Hash.fromStringArray(org.toString(), time.toString(), name),
      creation: time,
      last_modified: time,
      organizer: org,
      witnesses: [],
    });

    OpenedLaoStore.store(sampleLao);
  });

  /* NOTE: checks are done in checkRequests since msApi.request* return void */

  describe('network.WebsocketApi', () => {
    it('should create the correct request for requestCreateLao', async () => {
      await msApi.requestCreateLao(mockEventName);

      expect(publishMock.mock.calls.length).toBe(1);
      const [channel, msgData] = publishMock.mock.calls[0];
      expect(channel).toBe('/root');
      checkDataCreateLao(msgData);
    });

    it('should create the correct request for requestUpdateLao', async () => {
      await msApi.requestUpdateLao(mockEventName);

      expect(publishMock.mock.calls.length).toBe(1);
      const [channel, msgData] = publishMock.mock.calls[0];
      expect(channel).toBe(`/root/${sampleLao.id}`);
      checkDataUpdateLao(msgData);
    });

    it('should create the correct request for requestStateLao', async () => {
      await msApi.requestStateLao();

      expect(publishMock.mock.calls.length).toBe(1);
      const [channel, msgData] = publishMock.mock.calls[0];
      expect(channel).toBe(`/root/${sampleLao.id}`);
      checkDataStateLao(msgData);
    });

    it('should create the correct request for requestCreateMeeting 1', async () => {
      await msApi.requestCreateMeeting(mockEventName, mockStartTime);

      expect(publishMock.mock.calls.length).toBe(1);
      const [channel, msgData] = publishMock.mock.calls[0];
      expect(channel).toBe(`/root/${sampleLao.id}`);
      checkDataCreateMeeting(msgData);
    });

    it('should create the correct request for requestCreateMeeting 2', async () => {
      await msApi.requestCreateMeeting(mockEventName, mockStartTime, mockLocation);

      expect(publishMock.mock.calls.length).toBe(1);
      const [channel, msgData] = publishMock.mock.calls[0];
      expect(channel).toBe(`/root/${sampleLao.id}`);
      checkDataCreateMeeting(msgData);
    });

    it('should create the correct request for requestCreateMeeting 3', async () => {
      await msApi.requestCreateMeeting(mockEventName, mockStartTime, mockLocation, mockEndTime);

      expect(publishMock.mock.calls.length).toBe(1);
      const [channel, msgData] = publishMock.mock.calls[0];
      expect(channel).toBe(`/root/${sampleLao.id}`);
      checkDataCreateMeeting(msgData);
    });

    it('should create the correct request for requestCreateMeeting 4', async () => {
      const mockExtra = { numberParticipants: 12, minAge: 18 };
      await msApi.requestCreateMeeting(
        mockEventName, mockStartTime, mockLocation, mockEndTime, mockExtra,
      );

      expect(publishMock.mock.calls.length).toBe(1);
      const [channel, msgData] = publishMock.mock.calls[0];
      expect(channel).toBe(`/root/${sampleLao.id}`);
      checkDataCreateMeeting(msgData);
    });

    it('should create the correct request for requestWitnessMessage', async () => {
      await msApi.requestWitnessMessage(`/root/${sampleLao.id}`,
        Base64UrlData.encode('randomMessageId'));

      expect(publishMock.mock.calls.length).toBe(1);
      const [channel, msgData] = publishMock.mock.calls[0];
      expect(channel).toBe(`/root/${sampleLao.id}`);
      checkDataWitnessMessage(msgData);
    });

    it('should create the correct request for requestCreateRollCall', async () => {
      await msApi.requestCreateRollCall(mockEventName, mockLocation, mockStartTime, mockEndTime);

      expect(publishMock.mock.calls.length).toBe(1);
      const [channel, msgData] = publishMock.mock.calls[0];
      expect(channel).toBe(`/root/${sampleLao.id}`);
      checkDataCreateRollCall(msgData);
    });

    it('should create the correct request for requestCreateRollCall 2', async () => {
      const mockDescription = 'random description';
      await msApi.requestCreateRollCall(
        mockEventName, mockLocation, mockStartTime, mockEndTime, mockDescription,
      );

      expect(publishMock.mock.calls.length).toBe(1);
      const [channel, msgData] = publishMock.mock.calls[0];
      expect(channel).toBe(`/root/${sampleLao.id}`);
      checkDataCreateRollCall(msgData);
    });

    it('should create the correct request for requestOpenRollCall 1', async () => {
      await msApi.requestOpenRollCall(mockRollCallId);

      expect(publishMock.mock.calls.length).toBe(1);
      const [channel, msgData] = publishMock.mock.calls[0];
      expect(channel).toBe(`/root/${sampleLao.id}`);
      checkDataOpenRollCall(msgData);
    });

    it('should create the correct request for requestOpenRollCall 2', async () => {
      await msApi.requestOpenRollCall(mockRollCallId, mockStartTime);

      expect(publishMock.mock.calls.length).toBe(1);
      const [channel, msgData] = publishMock.mock.calls[0];
      expect(channel).toBe(`/root/${sampleLao.id}`);
      checkDataOpenRollCall(msgData);
    });

    it('should create the correct request for requestReopenRollCall 1', async () => {
      await msApi.requestReopenRollCall(mockRollCallId);

      expect(publishMock.mock.calls.length).toBe(1);
      const [channel, msgData] = publishMock.mock.calls[0];
      expect(channel).toBe(`/root/${sampleLao.id}`);
      checkDataReopenRollCall(msgData);
    });

    it('should create the correct request for requestReopenRollCall 2', async () => {
      await msApi.requestReopenRollCall(mockRollCallId, mockStartTime);

      expect(publishMock.mock.calls.length).toBe(1);
      const [channel, msgData] = publishMock.mock.calls[0];
      expect(channel).toBe(`/root/${sampleLao.id}`);
      checkDataReopenRollCall(msgData);
    });

    it('should create the correct request for requestCloseRollCall 1', async () => {
      await msApi.requestCloseRollCall(mockRollCallId, []);

      expect(publishMock.mock.calls.length).toBe(1);
      const [channel, msgData] = publishMock.mock.calls[0];
      expect(channel).toBe(`/root/${sampleLao.id}`);
      checkDataCloseRollCall(msgData);
    });

    it('should create the correct request for requestCloseRollCall 2', async () => {
      const attendeePks: PublicKey[] = [
        'NdCY6Ga7yVK5rZay3S4xGjlz2jHZQrg2gw7fyI6tfuo=',
        'BEW-uVz_NG_prXFuaKrI9Ae0EbBLGWehLQ8aLZFWY4w=',
      ].map((a) => new PublicKey(a));

      await msApi.requestCloseRollCall(mockRollCallId, attendeePks);

      expect(publishMock.mock.calls.length).toBe(1);
      const [channel, msgData] = publishMock.mock.calls[0];
      expect(channel).toBe(`/root/${sampleLao.id}`);

      const { attendees } = checkDataCloseRollCall(msgData);
      expect(attendees).toEqual(attendeePks);
    });

    it('should create the correct request for requestCloseRollCall 3', async () => {
      await msApi.requestCloseRollCall(mockRollCallId, [], mockEndTime);

      expect(publishMock.mock.calls.length).toBe(1);
      const [channel, msgData] = publishMock.mock.calls[0];
      expect(channel).toBe(`/root/${sampleLao.id}`);

      const msg = checkDataCloseRollCall(msgData);
      expect(msg.closed_at).toEqual(mockEndTime);
    });
  });
});
