import 'jest-extended';

import {
  configureTestFeatures,
  mockAddress,
  mockChannel,
  mockLaoId,
  mockPopToken,
} from '__tests__/utils';
import { ExtendedMessage } from 'core/network/ingestion/ExtendedMessage';
import { Timestamp } from 'core/objects';
import { AddChirp } from 'features/social/network/messages/chirp';

import { MessageRegistry } from '..';
import { configureMessages, Message } from '../Message';
import { ActionType, ObjectType, SignatureType } from '../MessageData';

const { CHIRP } = ObjectType;
const { ADD, INVALID } = ActionType;
const { POP_TOKEN } = SignatureType;

const messageData = new AddChirp({
  text: 'text',
  timestamp: new Timestamp(1607277600),
});

let registry: MessageRegistry;
beforeEach(() => {
  // setup fresh message registry for each test
  registry = configureTestFeatures();
  configureMessages(registry);
});

describe('MessageRegistry', () => {
  it('should throw an error when adding a handler to an unsupported type of message', async () => {
    const mockHandle = jest.fn();
    const mockBuild = jest.fn();
    const addWrongHandler = () => registry.add(CHIRP, INVALID, mockHandle, mockBuild);
    expect(addWrongHandler).toThrow(Error);
  });

  it('should work correctly for handling message', async () => {
    const message = Message.fromData(messageData, mockPopToken, mockChannel);
    const extMsg = ExtendedMessage.fromMessage(message, mockAddress, mockChannel);

    const mockHandle = jest.fn(() => true);
    const mockBuild = jest.fn();
    registry.add(CHIRP, ADD, mockHandle, mockBuild);

    expect(registry.handleMessage(extMsg)).toBeTrue();

    expect(mockHandle).toHaveBeenCalledTimes(1);
    expect(mockHandle).toHaveBeenCalledWith(extMsg);
    expect(mockBuild).not.toHaveBeenCalled();
  });

  it('should work correctly for building message data', async () => {
    const mockHandle = jest.fn();
    const mockBuild = jest.fn();
    registry.add(CHIRP, ADD, mockHandle, mockBuild);

    registry.buildMessageData(messageData, mockLaoId);

    expect(mockHandle).not.toHaveBeenCalled();
    expect(mockBuild).toHaveBeenCalledTimes(1);
    expect(mockBuild).toHaveBeenCalledWith(messageData, mockLaoId);
  });

  it('should throw an error when building an unsupported type of message', async () => {
    const buildWrongMessage = () =>
      registry.buildMessageData({ object: CHIRP, action: INVALID }, mockLaoId);
    expect(buildWrongMessage).toThrow(Error);
  });

  it('should throw an error when getting the signature of an unsupported type of message', () => {
    const wrongSignature = () => registry.getSignatureType({ object: CHIRP, action: INVALID });
    expect(wrongSignature).toThrow(Error);
  });

  it('should return the correct signature type', () => {
    expect(registry.getSignatureType(messageData)).toStrictEqual(POP_TOKEN);
  });

  it('verifyEntries should throw an error for undefined handler', () => {
    expect(registry.verifyEntries).toThrow(Error);
  });
});
