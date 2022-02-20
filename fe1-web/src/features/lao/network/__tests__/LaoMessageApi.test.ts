import 'jest-extended';
import '__tests__/utils/matchers';
import {
  configureTestFeatures,
  defaultMessageDataFields,
  mockLao,
  mockLaoId,
  mockLaoName,
} from '__tests__/utils';

import { ActionType, MessageData, ObjectType } from 'core/network/jsonrpc/messages';
import { KeyPairStore } from 'core/keypair';
import { Hash } from 'core/objects';
import { publish as mockPublish } from 'core/network/JsonRpcApi';

import { CreateLao, StateLao, UpdateLao } from '../messages';
import { OpenedLaoStore } from '../../store';
import * as msApi from '../LaoMessageApi';

jest.mock('network/JsonRpcApi');
const publishMock = mockPublish as jest.Mock;

let checkDataCreateLao: Function;
let checkDataUpdateLao: Function;
let checkDataStateLao: Function;

const initializeChecks = () => {
  checkDataCreateLao = (obj: MessageData) => {
    expect(obj.object).toBe(ObjectType.LAO);
    expect(obj.action).toBe(ActionType.CREATE);

    const data: CreateLao = obj as CreateLao;
    expect(data).toBeObject();
    expect(data).toContainKeys([
      ...defaultMessageDataFields,
      'id',
      'name',
      'creation',
      'organizer',
      'witnesses',
    ]);
    expect(data.id).toBeBase64Url();
    expect(data.name).toBeString();
    expect(data.name).toBe(mockLaoName);
    expect(data.creation).toBeNumberObject();
    expect(data.creation.valueOf()).toBeGreaterThan(0);
    expect(data.organizer).toBeBase64Url();
    expect(data.organizer).toBeJsonEqual(KeyPairStore.getPublicKey());
    expect(data.witnesses).toBeBase64UrlArray();
    expect(data.witnesses).toBeDistinctArray();

    // check id
    const expected: Hash = Hash.fromStringArray(
      data.organizer.toString(),
      data.creation.toString(),
      data.name,
    );
    expect(data.id).toBeJsonEqual(expected);
  };

  checkDataUpdateLao = (obj: MessageData) => {
    expect(obj.object).toBe(ObjectType.LAO);
    expect(obj.action).toBe(ActionType.UPDATE_PROPERTIES);

    const data: UpdateLao = obj as UpdateLao;
    expect(data).toBeObject();
    expect(data).toContainKeys([
      ...defaultMessageDataFields,
      'id',
      'name',
      'last_modified',
      'witnesses',
    ]);
    expect(data.id).toBeBase64Url();
    expect(data.name).toBeString();
    expect(data.name).toBe(mockLaoName);
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
  };

  checkDataStateLao = (obj: MessageData) => {
    expect(obj.object).toBe(ObjectType.LAO);
    expect(obj.action).toBe(ActionType.STATE);

    const data: StateLao = obj as StateLao;
    expect(data).toBeObject();
    expect(data).toContainKeys([
      ...defaultMessageDataFields,
      'id',
      'name',
      'creation',
      'last_modified',
      'organizer',
      'witnesses',
      'modification_id',
      'modification_signatures',
    ]);
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
      data.organizer.toString(),
      OpenedLaoStore.get().creation.toString(),
      data.name,
    );
    expect(data.id).toBeJsonEqual(expected);
  };
};

beforeAll(configureTestFeatures);

beforeEach(() => {
  OpenedLaoStore.store(mockLao);
  publishMock.mockClear();
  initializeChecks();
});

describe('MessageApi', () => {
  it('should create the correct request for requestCreateLao', async () => {
    await msApi.requestCreateLao(mockLaoName);

    expect(publishMock).toBeCalledTimes(1);
    const [channel, msgData] = publishMock.mock.calls[0];
    expect(channel).toBe('/root');
    checkDataCreateLao(msgData);
  });

  it('should create the correct request for requestUpdateLao', async () => {
    await msApi.requestUpdateLao(mockLaoName);

    expect(publishMock).toBeCalledTimes(1);
    const [channel, msgData] = publishMock.mock.calls[0];
    expect(channel).toBe(`/root/${mockLaoId}`);
    checkDataUpdateLao(msgData);
  });

  it('should create the correct request for requestStateLao', async () => {
    await msApi.requestStateLao();

    expect(publishMock).toBeCalledTimes(1);
    const [channel, msgData] = publishMock.mock.calls[0];
    expect(channel).toBe(`/root/${mockLaoId}`);
    checkDataStateLao(msgData);
  });
});
