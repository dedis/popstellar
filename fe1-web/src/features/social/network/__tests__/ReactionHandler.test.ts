import 'jest-extended';
import '__tests__/utils/matchers';

import { mockAddress, mockKeyPair, mockLaoId } from '__tests__/utils';
import { ActionType, ObjectType, ProcessableMessage } from 'core/network/jsonrpc/messages';
import { Base64UrlData, getGeneralChirpsChannel, Hash, Signature, Timestamp } from 'core/objects';
import { dispatch } from 'core/redux';
import { Reaction } from 'features/social/objects';
import { addReaction } from 'features/social/reducer';

import { AddReaction } from '../messages/reaction';
import { handleAddReactionMessage } from '../ReactionHandler';

const TIMESTAMP = new Timestamp(1609455600); // 1st january 2021

const mockMessageId = new Hash('some string');
const mockSender = mockKeyPair.publicKey;
const mockAddReaction = new AddReaction({
  chirp_id: mockMessageId,
  reaction_codepoint: '👍',
  timestamp: TIMESTAMP,
});

const mockMessageData = {
  receivedAt: TIMESTAMP,
  receivedFrom: mockAddress,
  laoId: mockLaoId,
  data: Base64UrlData.encode('some data'),
  sender: mockSender,
  signature: Base64UrlData.encode('some data') as Signature,
  channel: getGeneralChirpsChannel(mockLaoId),
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

describe('handleAddReactionMessage', () => {
  it('should return false if the object type is wrong', () => {
    expect(
      handleAddReactionMessage(getCurrentLaoId)({
        ...mockMessageData,
        messageData: {
          object: ObjectType.CHIRP,
          action: ActionType.ADD,
        },
      } as ProcessableMessage),
    ).toBeFalse();
  });

  it('should return false if the action type is wrong', () => {
    expect(
      handleAddReactionMessage(getCurrentLaoId)({
        ...mockMessageData,
        messageData: {
          object: ObjectType.REACTION,
          action: ActionType.DELETE,
        },
      } as ProcessableMessage),
    ).toBeFalse();
  });

  it('should return false if there is no current lao', () => {
    expect(
      handleAddReactionMessage(() => undefined)({
        ...mockMessageData,
        messageData: mockAddReaction,
      }),
    ).toBeFalse();
  });

  it('should return false if there is an issue with the message data', () => {
    expect(
      handleAddReactionMessage(getCurrentLaoId)({
        ...mockMessageData,
        messageData: {
          object: ObjectType.CHIRP,
          action: ActionType.ADD,
          chirp_id: undefined as unknown as Hash,
          reaction_codepoint: '👍',
          timestamp: TIMESTAMP,
        } as AddReaction,
      }),
    ).toBeFalse();
  });

  it('should return true for valid messages', () => {
    expect(
      handleAddReactionMessage(getCurrentLaoId)({
        ...mockMessageData,
        messageData: mockAddReaction,
      }),
    ).toBeTrue();

    expect(dispatch).toHaveBeenCalledWith(
      addReaction(
        mockLaoId,
        new Reaction({
          id: mockMessageId,
          sender: mockSender,
          codepoint: mockAddReaction.reaction_codepoint,
          chirpId: mockAddReaction.chirp_id,
          time: mockAddReaction.timestamp,
        }),
      ),
    );
    expect(dispatch).toHaveBeenCalledTimes(1);
  });
});
