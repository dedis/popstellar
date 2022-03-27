/* eslint-disable @typescript-eslint/dot-notation */
import 'jest-extended';

import { configureTestFeatures, mockPopToken } from '__tests__/utils';
import { ExtendedMessage } from 'core/network/ingestion/ExtendedMessage';
import { channelFromIds, Timestamp } from 'core/objects';
import { Lao, LaoState } from 'features/lao/objects';
import { OpenedLaoStore } from 'features/lao/store';
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
const channel = channelFromIds();
const laoState: LaoState = {
  id: 'mockLaoId',
  name: 'MyLao',
  creation: 1577833300,
  last_modified: 1577833500,
  organizer: 'organizerPublicKey',
  witnesses: [],
  server_addresses: [],
};
const getMock = jest.spyOn(OpenedLaoStore, 'get');
getMock.mockImplementation(() => Lao.fromState(laoState));

let registry: MessageRegistry;
beforeEach(() => {
  // setup fresh message registry for each test
  registry = configureTestFeatures();
  configureMessages(registry);

  // clear any handlers that might be initialized
  registry['afterProcessingHandlers'] = [];
});

describe('MessageRegistry', () => {
  it('should throw an error when adding a handler to an unsupported type of message', async () => {
    const mockHandle = jest.fn();
    const mockBuild = jest.fn();
    const addWrongHandler = () => registry.add(CHIRP, INVALID, mockHandle, mockBuild);
    expect(addWrongHandler).toThrow(Error);
  });

  it('should work correctly for handling message', async () => {
    const message = Message.fromData(messageData, mockPopToken);
    const extMsg = ExtendedMessage.fromMessage(message, channel, 'some address');

    const mockHandle = jest.fn().mockImplementation(() => true);
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

    registry.buildMessageData(messageData);

    expect(mockHandle).not.toHaveBeenCalled();
    expect(mockBuild).toHaveBeenCalledTimes(1);
    expect(mockBuild).toHaveBeenCalledWith(messageData);
  });

  it('should throw an error when building an unsupported type of message', async () => {
    const buildWrongMessage = () => registry.buildMessageData({ object: CHIRP, action: INVALID });
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

  it('is initialized with no after processing handlers', () => {
    expect(registry['afterProcessingHandlers']).toEqual([]);
  });

  it('is possible to add after processing handlers', () => {
    const handler1 = jest.fn();
    const handler2 = jest.fn();

    registry.addAfterProcessingHandler(handler1);
    registry.addAfterProcessingHandler(handler2);

    expect(registry['afterProcessingHandlers']).toEqual([handler1, handler2]);
  });

  it('is possible to remove after processing handlers', () => {
    const handler1 = jest.fn();
    const handler2 = jest.fn();

    registry.addAfterProcessingHandler(handler1);
    registry.addAfterProcessingHandler(handler2);
    registry.removeAfterProcessingHandler(handler1);

    expect(registry['afterProcessingHandlers']).toEqual([handler2]);
  });

  it('is possible to clear all processing handlers', () => {
    const handler1 = jest.fn();
    const handler2 = jest.fn();

    registry.addAfterProcessingHandler(handler1);
    registry.addAfterProcessingHandler(handler2);
    registry.clearAfterProcessingHandler();

    expect(registry['afterProcessingHandlers']).toEqual([]);
  });

  it('executes the after processing handler after successfully processing a message', () => {
    const handler1 = jest.fn();
    const handler2 = jest.fn();

    registry.addAfterProcessingHandler(handler1);
    registry.addAfterProcessingHandler(handler2);

    const message = Message.fromData(messageData, mockPopToken);
    const extMsg = ExtendedMessage.fromMessage(message, channel, 'some address');

    const mockHandle = jest.fn().mockImplementation(() => true);
    const mockBuild = jest.fn();
    registry.add(CHIRP, ADD, mockHandle, mockBuild);

    expect(registry.handleMessage(extMsg)).toBeTrue();

    expect(handler1).toHaveBeenCalledWith(extMsg);
    expect(handler1).toHaveBeenCalledTimes(1);

    expect(handler2).toHaveBeenCalledWith(extMsg);
    expect(handler2).toHaveBeenCalledTimes(1);
  });

  it('does not execute the after processing handler after non-successfull handling of a message', () => {
    const handler1 = jest.fn();
    const handler2 = jest.fn();

    registry.addAfterProcessingHandler(handler1);
    registry.addAfterProcessingHandler(handler2);

    const message = Message.fromData(messageData, mockPopToken);
    const extMsg = ExtendedMessage.fromMessage(message, channel, 'some address');

    const mockHandle = jest.fn().mockImplementation(() => false);
    const mockBuild = jest.fn();
    registry.add(CHIRP, ADD, mockHandle, mockBuild);

    expect(registry.handleMessage(extMsg)).toBeFalse();

    expect(handler1).toHaveBeenCalledTimes(0);
    expect(handler2).toHaveBeenCalledTimes(0);
  });
});
