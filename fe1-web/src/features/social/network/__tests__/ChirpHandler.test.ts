import 'jest-extended';
import '__tests__/utils/matchers';

import { mockAddress, mockKeyPair, mockLaoId } from '__tests__/utils';
import { ActionType, ObjectType, ProcessableMessage } from 'core/network/jsonrpc/messages';
import { Base64UrlData, getGeneralChirpsChannel, Hash, Signature, Timestamp } from 'core/objects';
import { dispatch } from 'core/redux';
import { Chirp } from 'features/social/objects';
import { addChirp, deleteChirp } from 'features/social/reducer';

import { handleAddChirpMessage, handleDeleteChirpMessage } from '../ChirpHandler';
import { AddChirp, DeleteChirp } from '../messages/chirp';

const TIMESTAMP = new Timestamp(1609455600); // 1st january 2021

const mockMessageId = new Hash('some string');
const mockSender = mockKeyPair.publicKey;
const mockAddChirp = new AddChirp({ text: 'text', timestamp: TIMESTAMP });
const mockDeleteChirp = new DeleteChirp({ chirp_id: mockMessageId, timestamp: TIMESTAMP });

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

describe('handleAddChirpMessage', () => {
  it('should return false if the object type is wrong', () => {
    expect(
      handleAddChirpMessage(getCurrentLaoId)({
        ...mockMessageData,
        messageData: {
          object: ObjectType.REACTION,
          action: ActionType.ADD,
        },
      } as ProcessableMessage),
    ).toBeFalse();
  });

  it('should return false if the action type is wrong', () => {
    expect(
      handleAddChirpMessage(getCurrentLaoId)({
        ...mockMessageData,
        messageData: {
          object: ObjectType.CHIRP,
          action: ActionType.DELETE,
        },
      } as ProcessableMessage),
    ).toBeFalse();
  });

  it('should return false if there is no current lao', () => {
    expect(
      handleAddChirpMessage(() => undefined)({
        ...mockMessageData,
        messageData: mockAddChirp,
      }),
    ).toBeFalse();
  });

  it('should return false if there is an issue with the message data', () => {
    expect(
      handleAddChirpMessage(getCurrentLaoId)({
        ...mockMessageData,
        messageData: {
          object: ObjectType.CHIRP,
          action: ActionType.ADD,
          text: undefined as unknown as string,
          timestamp: TIMESTAMP,
        } as AddChirp,
      }),
    ).toBeFalse();
  });

  it('should return true for valid messages', () => {
    expect(
      handleAddChirpMessage(getCurrentLaoId)({
        ...mockMessageData,
        messageData: mockAddChirp,
      }),
    ).toBeTrue();

    expect(dispatch).toHaveBeenCalledWith(
      addChirp(
        mockLaoId,
        new Chirp({
          id: mockMessageId,
          sender: mockSender,
          text: mockAddChirp.text,
          time: mockAddChirp.timestamp,
          parentId: mockAddChirp.parent_id,
        }),
      ),
    );
    expect(dispatch).toHaveBeenCalledTimes(1);
  });
});

describe('handleDeleteChirpMessage', () => {
  it('should return false if the object type is wrong', () => {
    expect(
      handleDeleteChirpMessage(getCurrentLaoId)({
        ...mockMessageData,
        messageData: {
          object: ObjectType.REACTION,
          action: ActionType.DELETE,
        },
      } as ProcessableMessage),
    ).toBeFalse();
  });

  it('should return false if the action type is wrong', () => {
    expect(
      handleDeleteChirpMessage(getCurrentLaoId)({
        ...mockMessageData,
        messageData: {
          object: ObjectType.CHIRP,
          action: ActionType.ADD,
        },
      } as ProcessableMessage),
    ).toBeFalse();
  });

  it('should return false if there is no current lao', () => {
    expect(
      handleDeleteChirpMessage(() => undefined)({
        ...mockMessageData,
        messageData: mockDeleteChirp,
      }),
    ).toBeFalse();
  });

  it('should return false if there is an issue with the message data', () => {
    expect(
      handleDeleteChirpMessage(getCurrentLaoId)({
        ...mockMessageData,
        messageData: {
          object: ObjectType.CHIRP,
          action: ActionType.DELETE,
          chirp_id: undefined as unknown as Hash,
          timestamp: undefined as unknown as Timestamp,
        } as DeleteChirp,
      }),
    ).toBeFalse();
  });

  it('should return true for valid messages', () => {
    expect(
      handleDeleteChirpMessage(getCurrentLaoId)({
        ...mockMessageData,
        messageData: mockDeleteChirp,
      }),
    ).toBeTrue();

    expect(dispatch).toHaveBeenCalledWith(
      deleteChirp(
        mockLaoId,
        new Chirp({
          id: mockMessageId,
          sender: mockSender,
          time: mockDeleteChirp.timestamp,
          text: '',
        }),
      ),
    );
    expect(dispatch).toHaveBeenCalledTimes(1);
  });
});
