import 'jest-extended';
import '__tests__/utils/matchers';

import {
  configureTestFeatures,
  serializedMockLaoId,
  mockLaoId,
  mockLaoId2,
  mockKeyPair,
  mockLaoServerAddress,
} from '__tests__/utils';
import { ActionType, Message, MessageData, ObjectType } from 'core/network/jsonrpc/messages';
import { publish as mockPublish } from 'core/network/JsonRpcApi';
import { Hash, Timestamp } from 'core/objects';
import { OpenedLaoStore } from 'features/lao/store';
import { Challenge, ChallengeState } from 'features/linked-organizations/objects/Challenge';
import { mockRollCall } from 'features/rollCall/__tests__/utils';

import * as msApi from '../LinkedOrgMessageApi';
import { ChallengeRequest } from '../messages';
import { FederationExpect } from '../messages/FederationExpect';
import { FederationInit } from '../messages/FederationInit';
import { TokensExchange } from '../messages/TokensExchange';

jest.mock('core/network/JsonRpcApi', () => {
  return {
    getSigningKeyPair: jest.fn(() => Promise.resolve(mockKeyPair)),
    publish: jest.fn(),
  };
});
// Type casting to ensure Jest recognizes the mock functions
const publishMock = mockPublish as jest.MockedFunction<typeof mockPublish>;

const VALID_HASH_VALUE = new Hash(
  '8c01ef1ff091d3ec5650a0d40f9fbdb911606a32c2252144eecbe30235a1d938',
);
const VALID_TIMESTAMP = new Timestamp(1716306892);

const checkDataChallengeRequest = (obj: MessageData) => {
  expect(obj.object).toBe(ObjectType.FEDERATION);
  expect(obj.action).toBe(ActionType.CHALLENGE_REQUEST);

  const data: ChallengeRequest = obj as ChallengeRequest;
  expect(data).toBeObject();
  expect(data.timestamp).toBeNumberObject();
};

const checkDataFederationInit = (obj: MessageData) => {
  expect(obj.object).toBe(ObjectType.FEDERATION);
  expect(obj.action).toBe(ActionType.FEDERATION_INIT);

  const data: FederationInit = obj as FederationInit;
  expect(data).toBeObject();
  expect(data.lao_id).toBeBase64Url();
  expect(data.server_address).toBeString();
  expect(data.public_key).toBeBase64Url();
  expect(data.challenge).toBeInstanceOf(Message);
};

const checkDataFederationExpect = (obj: MessageData) => {
  expect(obj.object).toBe(ObjectType.FEDERATION);
  expect(obj.action).toBe(ActionType.FEDERATION_EXPECT);

  const data: FederationExpect = obj as FederationExpect;
  expect(data).toBeObject();
  expect(data.lao_id).toBeBase64Url();
  expect(data.server_address).toBeString();
  expect(data.public_key).toBeBase64Url();
  expect(data.challenge).toBeInstanceOf(Message);
};

const checkDataTokensExchange = (obj: MessageData) => {
  expect(obj.object).toBe(ObjectType.FEDERATION);
  expect(obj.action).toBe(ActionType.TOKENS_EXCHANGE);

  const data: TokensExchange = obj as TokensExchange;
  expect(data).toBeObject();
  expect(data.lao_id).toBeBase64Url();
  expect(data.roll_call_id).toBeBase64Url();
  expect(data.tokens).toBeArray();
  expect(data.timestamp).toBeNumberObject();
};

beforeAll(configureTestFeatures);

beforeEach(() => {
  jest.clearAllMocks();
  OpenedLaoStore.store(mockLaoServerAddress);
});

describe('LinkedOrgMessageApi', () => {
  it('should create the correct request for requestChallenge', async () => {
    await msApi.requestChallenge(mockLaoId);

    expect(publishMock).toBeCalledTimes(1);
    const [channel, msgData] = publishMock.mock.calls[0];
    expect(channel).toBe(`/root/${serializedMockLaoId}/federation`);
    checkDataChallengeRequest(msgData);
  });

  it('should create the correct request for initFederation', async () => {
    const challengeState: ChallengeState = {
      value: VALID_HASH_VALUE.toState(),
      valid_until: VALID_TIMESTAMP.valueOf(),
    };
    const challenge = Challenge.fromState(challengeState);
    await msApi.initFederation(
      mockLaoId2,
      mockLaoServerAddress.id,
      mockLaoServerAddress.server_addresses.at(0)!,
      mockLaoServerAddress.organizer,
      challenge,
    );

    expect(publishMock).toBeCalledTimes(1);
    const [channel, msgData] = publishMock.mock.calls[0];
    expect(channel).toBe(`/root/${mockLaoId2.valueOf()}/federation`);
    checkDataFederationInit(msgData);
  });

  it('should create the correct request for expectFederation', async () => {
    const challengeState: ChallengeState = {
      value: VALID_HASH_VALUE.toState(),
      valid_until: VALID_TIMESTAMP.valueOf(),
    };
    const challenge = Challenge.fromState(challengeState);
    await msApi.expectFederation(
      mockLaoId2,
      mockLaoServerAddress.id,
      mockLaoServerAddress.server_addresses.at(0)!,
      mockLaoServerAddress.organizer,
      challenge,
    );

    expect(publishMock).toBeCalledTimes(1);
    const [channel, msgData] = publishMock.mock.calls[0];
    expect(channel).toBe(`/root/${mockLaoId2.valueOf()}/federation`);
    checkDataFederationExpect(msgData);
  });

  it('should create the correct request for tokenExchange', async () => {
    const mockTokensExchange = new TokensExchange({
      lao_id: mockLaoId2,
      roll_call_id: mockRollCall.id,
      tokens: mockRollCall.attendees,
      timestamp: VALID_TIMESTAMP,
    });
    await msApi.tokensExchange(
      mockLaoId,
      mockTokensExchange.lao_id,
      mockTokensExchange.roll_call_id,
      mockTokensExchange.tokens,
    );

    expect(publishMock).toBeCalledTimes(1);
    const [channel, msgData] = publishMock.mock.calls[0];
    expect(channel).toBe(`/root/${mockLaoId.valueOf()}/federation`);
    checkDataTokensExchange(msgData);
  });
});
