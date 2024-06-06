import 'jest-extended';
import '__tests__/utils/matchers';

import { mockAddress, mockKeyPair, mockLaoId, mockLaoServerAddress } from '__tests__/utils';
import { ActionType, Message, ObjectType, ProcessableMessage } from 'core/network/jsonrpc/messages';
import {
  Base64UrlData,
  getFederationChannel,
  Hash,
  PublicKey,
  Signature,
  Timestamp,
} from 'core/objects';
import { dispatch } from 'core/redux';
import { Challenge } from 'features/linked-organizations/objects/Challenge';
import { setChallenge } from 'features/linked-organizations/reducer';

import {
  handleChallengeMessage,
  handleChallengeRequestMessage,
  handleFederationExpectMessage,
  handleFederationInitMessage,
} from '../LinkedOrgHandler';
import {
  ChallengeRequest,
  ChallengeMessage,
  FederationExpect,
  FederationInit,
  FederationResult,
} from '../messages';

jest.mock('core/network/jsonrpc/messages/Message', () => {
  return {
    Message: jest.fn().mockImplementation((x) => {
      return {
        buildMessageData: jest.fn(() => JSON.parse(JSON.stringify(x))),
      };
    }),
  };
});

const TIMESTAMP = new Timestamp(1609455600); // 1st january 2021

const mockMessageId = new Hash('some string');
const mockSender = mockKeyPair.publicKey;
const mockChallengeRequest = new ChallengeRequest({ timestamp: TIMESTAMP });
const mockChallengeMessage = new ChallengeMessage({
  value: new Hash('8c01ef1ff091d3ec5650a0d40f9fbdb911606a32c2252144eecbe30235a1d938'),
  valid_until: TIMESTAMP,
});
const mockChallengMessageData = new Message(
  {
    data: new Base64UrlData(
      'eyJvYmplY3QiOiJmZWRlcmF0aW9uIiwiYWN0aW9uIjoiY2hhbGxlbmdlIiwidmFsdWUiOiJkMWU4YTgyYTFlOTRjOTIyNWU3MTc5YTgxMmJmZGUyNDQ2ZTQzOGRmYjA0ZGQ5ZGE5MzRkN2Y3ZjBhZjk4MGRiIiwidmFsaWRfdW50aWwiOjE3MTYzMTMxMTB9',
    ),
    sender: new PublicKey('jV1IQlPWlh1bSUL9NAtGeTWfIpZvZXdye7GZ1vmPOHw='),
    signature: new Signature(
      'XMuS6chzK3YQglfRxVM2FBM7rT0oJhog_HtHvvf_5WKCuUPV8wWzbPC18eFRvr5ojHDidd_MMEp-k9KTrqLJDw==',
    ),
    message_id: new Hash('ndPGYZawpgT26K_eiDA6vsjlsJ20G_NSdrQ0uiGBHzA='),
    witness_signatures: [],
  },
  getFederationChannel(mockLaoServerAddress.id),
);
const mockFederationInit = new FederationInit({
  lao_id: mockLaoServerAddress.id,
  server_address: mockLaoServerAddress.server_addresses.at(0),
  public_key: mockLaoServerAddress.organizer,
  challenge: mockChallengMessageData,
});
const mockFederationExpect = new FederationExpect({
  lao_id: mockLaoServerAddress.id,
  server_address: mockLaoServerAddress.server_addresses.at(0),
  public_key: mockLaoServerAddress.organizer,
  challenge: mockChallengMessageData,
});

const mockMessageData = {
  receivedAt: TIMESTAMP,
  receivedFrom: mockAddress,
  laoId: mockLaoId,
  data: Base64UrlData.encode('some data'),
  sender: mockSender,
  signature: Base64UrlData.encode('some data') as Signature,
  channel: getFederationChannel(mockLaoId),
  message_id: mockMessageId,
  witness_signatures: [],
};

const getCurrentLaoId = () => mockLaoId;

jest.mock('core/redux', () => {
  const actualModule = jest.requireActual('core/redux');
  return {
    ...actualModule,
    dispatch: jest.fn((...args) => actualModule.dispatch(...args)),
  };
});

beforeEach(() => {
  jest.clearAllMocks();
});

describe('handleChallengeRequestMessage', () => {
  it('should return false if the object type is wrong', () => {
    expect(
      handleChallengeRequestMessage(getCurrentLaoId)({
        ...mockMessageData,
        messageData: {
          object: ObjectType.CHIRP,
          action: ActionType.CHALLENGE_REQUEST,
        },
      } as ProcessableMessage),
    ).toBeFalse();
  });

  it('should return false if the action type is wrong', () => {
    expect(
      handleChallengeRequestMessage(getCurrentLaoId)({
        ...mockMessageData,
        messageData: {
          object: ObjectType.FEDERATION,
          action: ActionType.SETUP,
        },
      } as ProcessableMessage),
    ).toBeFalse();
  });

  it('should return false if there is no current lao', () => {
    expect(
      handleChallengeRequestMessage(() => undefined)({
        ...mockMessageData,
        messageData: mockChallengeRequest,
      }),
    ).toBeFalse();
  });

  it('should return true for valid messages', () => {
    expect(
      handleChallengeRequestMessage(getCurrentLaoId)({
        ...mockMessageData,
        messageData: mockChallengeRequest,
      }),
    ).toBeTrue();
  });
});

describe('handleChallengeMessage', () => {
  it('should return false if the object type is wrong', () => {
    expect(
      handleChallengeMessage()({
        ...mockMessageData,
        messageData: {
          object: ObjectType.MEETING,
          action: ActionType.CHALLENGE,
        },
      } as ProcessableMessage),
    ).toBeFalse();
  });

  it('should return false if the action type is wrong', () => {
    expect(
      handleChallengeMessage()({
        ...mockMessageData,
        messageData: {
          object: ObjectType.FEDERATION,
          action: ActionType.ADD,
        },
      } as ProcessableMessage),
    ).toBeFalse();
  });

  it('should return false if there is an issue with the message data', () => {
    expect(
      handleChallengeMessage()({
        ...mockMessageData,
        messageData: {
          object: ObjectType.FEDERATION,
          action: ActionType.CHALLENGE,
          value: undefined as unknown as Hash,
          valid_until: undefined as unknown as Timestamp,
        } as ChallengeMessage,
      }),
    ).toBeFalse();
  });

  it('should return true for valid messages', () => {
    expect(
      handleChallengeMessage()({
        ...mockMessageData,
        messageData: mockChallengeMessage,
      }),
    ).toBeTrue();

    expect(dispatch).toHaveBeenCalledWith(
      setChallenge(
        mockLaoId,
        new Challenge({
          value: mockChallengeMessage.value as Hash,
          valid_until: mockChallengeMessage.valid_until as Timestamp,
        }).toState(),
      ),
    );
    expect(dispatch).toHaveBeenCalledTimes(1);
  });
});

describe('handleFederationInitMessage', () => {
  it('should return false if the object type is wrong', () => {
    expect(
      handleFederationInitMessage(getCurrentLaoId)({
        ...mockMessageData,
        messageData: {
          object: ObjectType.MEETING,
          action: ActionType.FEDERATION_INIT,
        },
      } as ProcessableMessage),
    ).toBeFalse();
  });

  it('should return false if the action type is wrong', () => {
    expect(
      handleFederationInitMessage(getCurrentLaoId)({
        ...mockMessageData,
        messageData: {
          object: ObjectType.FEDERATION,
          action: ActionType.ADD,
        },
      } as ProcessableMessage),
    ).toBeFalse();
  });

  it('should return false if there is no current lao', () => {
    expect(
      handleFederationInitMessage(() => undefined)({
        ...mockMessageData,
        messageData: mockFederationInit,
      }),
    ).toBeFalse();
  });

  it('should return false if there is an issue with the message data', () => {
    expect(
      handleFederationInitMessage(getCurrentLaoId)({
        ...mockMessageData,
        messageData: {
          object: ObjectType.FEDERATION,
          action: ActionType.FEDERATION_INIT,
          lao_id: undefined as unknown as Hash,
          server_address: undefined as unknown as String,
          public_key: undefined as unknown as Hash,
          challenge: undefined as unknown as Message,
        } as FederationInit,
      }),
    ).toBeFalse();
  });

  it('should return true for valid messages', () => {
    expect(
      handleFederationInitMessage(getCurrentLaoId)({
        ...mockMessageData,
        messageData: mockFederationInit,
      }),
    ).toBeTrue();
  });
});

describe('handleFederationExpectMessage', () => {
  it('should return false if the object type is wrong', () => {
    expect(
      handleFederationExpectMessage(getCurrentLaoId)({
        ...mockMessageData,
        messageData: {
          object: ObjectType.MEETING,
          action: ActionType.FEDERATION_EXPECT,
        },
      } as ProcessableMessage),
    ).toBeFalse();
  });

  it('should return false if the action type is wrong', () => {
    expect(
      handleFederationExpectMessage(getCurrentLaoId)({
        ...mockMessageData,
        messageData: {
          object: ObjectType.FEDERATION,
          action: ActionType.ADD,
        },
      } as ProcessableMessage),
    ).toBeFalse();
  });

  it('should return false if there is no current lao', () => {
    expect(
      handleFederationExpectMessage(() => undefined)({
        ...mockMessageData,
        messageData: mockFederationExpect,
      }),
    ).toBeFalse();
  });

  it('should return false if there is an issue with the message data', () => {
    expect(
      handleFederationExpectMessage(getCurrentLaoId)({
        ...mockMessageData,
        messageData: {
          object: ObjectType.FEDERATION,
          action: ActionType.FEDERATION_EXPECT,
          lao_id: undefined as unknown as Hash,
          server_address: undefined as unknown as String,
          public_key: undefined as unknown as Hash,
          challenge: undefined as unknown as Message,
        } as FederationExpect,
      }),
    ).toBeFalse();
  });

  it('should return true for valid messages', () => {
    expect(
      handleFederationExpectMessage(getCurrentLaoId)({
        ...mockMessageData,
        messageData: mockFederationExpect,
      }),
    ).toBeTrue();
  });
});

describe('handleFederationResultMessage', () => {
  it('should return false if the object type is wrong', () => {
    expect(
      handleChallengeMessage()({
        ...mockMessageData,
        messageData: {
          object: ObjectType.MEETING,
          action: ActionType.FEDERATION_RESULT,
        },
      } as ProcessableMessage),
    ).toBeFalse();
  });

  it('should return false if the action type is wrong', () => {
    expect(
      handleChallengeMessage()({
        ...mockMessageData,
        messageData: {
          object: ObjectType.FEDERATION,
          action: ActionType.ADD,
        },
      } as ProcessableMessage),
    ).toBeFalse();
  });

  it('should return false if there is an issue with the message data', () => {
    expect(
      handleChallengeMessage()({
        ...mockMessageData,
        messageData: {
          object: ObjectType.FEDERATION,
          action: ActionType.FEDERATION_RESULT,
          challenge: undefined as unknown as Message,
          status: undefined as unknown as string,
          public_key: undefined as unknown as PublicKey,
        } as FederationResult,
      }),
    ).toBeFalse();
  });
  it('should return false if there is both a public key and a reason in the message data', () => {
    expect(
      handleChallengeMessage()({
        ...mockMessageData,
        messageData: {
          object: ObjectType.FEDERATION,
          action: ActionType.FEDERATION_RESULT,
          challenge: mockChallengMessageData,
          status: 'success',
          reason: 'error',
          public_key: mockSender,
        } as FederationResult,
      }),
    ).toBeFalse();
  });
});
