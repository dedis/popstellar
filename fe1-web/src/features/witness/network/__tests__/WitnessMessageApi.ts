import 'jest-extended';
import '__tests__/utils/matchers';

import { defaultMessageDataFields, mockLao, mockLaoId } from '__tests__/utils/TestUtils';
import { ActionType, MessageData, ObjectType } from 'core/network/jsonrpc/messages';
import { publish as mockPublish } from 'core/network/JsonRpcApi';
import { Base64UrlData } from 'core/objects';
import { OpenedLaoStore } from 'features/lao/store';

import { WitnessMessage } from '../messages';
import * as msApi from '../WitnessMessageApi';

jest.mock('core/network/JsonRpcApi');
const publishMock = mockPublish as jest.Mock;

const checkDataWitnessMessage = (obj: MessageData) => {
  expect(obj.object).toBe(ObjectType.MESSAGE);
  expect(obj.action).toBe(ActionType.WITNESS);

  const data: WitnessMessage = obj as WitnessMessage;
  expect(data).toContainKeys([...defaultMessageDataFields, 'message_id', 'signature']);
  expect(data.message_id).toBeBase64Url();
  expect(data.signature).toBeBase64Url();
};

beforeEach(() => {
  OpenedLaoStore.store(mockLao);
  publishMock.mockClear();
});

describe('MessageApi', () => {
  it('should create the correct request for requestWitnessMessage', async () => {
    await msApi.requestWitnessMessage(
      `/root/${mockLaoId}`,
      Base64UrlData.encode('randomMessageId'),
    );

    expect(publishMock).toBeCalledTimes(1);
    const [channel, msgData] = publishMock.mock.calls[0];
    expect(channel).toBe(`/root/${mockLaoId}`);
    checkDataWitnessMessage(msgData);
  });
});
