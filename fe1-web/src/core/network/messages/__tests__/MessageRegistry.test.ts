import 'jest-extended';
import { channelFromIds, Timestamp } from 'core/objects';
import { ExtendedMessage } from 'core/network/messages/ExtendedMessage';
import { configureMessages, Message } from 'core/network/messages/Message';
import { AddChirp } from 'features/social/network/messages/chirp';
import { OpenedLaoStore } from 'features/lao/store';
import { Lao, LaoState } from 'features/lao/objects';
import { MessageRegistry } from 'core/network/messages/MessageRegistry';
import { ActionType, ObjectType, SignatureType } from 'core/network/messages/MessageData';

const { CHIRP } = ObjectType;
const { ADD, INVALID } = ActionType;
const { POP_TOKEN } = SignatureType;
let registry: MessageRegistry;
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
};
const getMock = jest.spyOn(OpenedLaoStore, 'get');
getMock.mockImplementation(() => Lao.fromState(laoState));

beforeEach(() => {
  registry = new MessageRegistry();
  configureMessages(registry);
});

describe('MessageRegistry', () => {
  it('should throw an error when adding a handler to an unsupported type of message', async () => {
    const mockHandle = jest.fn();
    const addWrongHandler = () => registry.addHandler(CHIRP, INVALID, mockHandle);
    expect(addWrongHandler).toThrow(Error);
  });

  it('should work correctly for handling message', async () => {
    const mockHandle = jest.fn();
    registry.addHandler(CHIRP, ADD, mockHandle);
    const message = await Message.fromData(messageData);
    const extMsg = ExtendedMessage.fromMessage(message, channel);
    registry.handleMessage(extMsg);
    expect(mockHandle).toHaveBeenCalledTimes(1);
    expect(mockHandle).toHaveBeenCalledWith(extMsg);
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
});
