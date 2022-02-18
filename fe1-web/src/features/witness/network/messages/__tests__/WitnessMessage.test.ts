import 'jest-extended';
import { sign } from 'tweetnacl';

import '__tests__/utils/matchers';
import { Base64UrlData, KeyPair, PrivateKey, PublicKey, ProtocolError } from 'core/objects';
import { mockPrivateKey } from '__tests__/utils/TestUtils';
import { ActionType, ObjectType } from 'core/network/messages/MessageData';

import { WitnessMessage } from '../WitnessMessage';

const mockMessageId = Base64UrlData.encode('message_id');
const mockSecretKey = new PrivateKey(mockPrivateKey);
const mockSignature = mockSecretKey.sign(mockMessageId);

const sampleWitnessMessage: Partial<WitnessMessage> = {
  object: ObjectType.MESSAGE,
  action: ActionType.WITNESS,
  message_id: mockMessageId,
  signature: mockSignature,
};

const witnessMessageJson = `{
  "object": "${ObjectType.MESSAGE}",
  "action": "${ActionType.WITNESS}",
  "message_id": "${mockMessageId}",
  "signature": "${mockSignature}"
}`;

describe('WitnessMessage', () => {
  it('should be created correctly from Json', () => {
    expect(new WitnessMessage(sampleWitnessMessage)).toBeJsonEqual(sampleWitnessMessage);
    const temp = {
      object: ObjectType.MESSAGE,
      action: ActionType.WITNESS,
      message_id: mockMessageId,
      signature: mockSignature,
    };
    expect(new WitnessMessage(temp)).toBeJsonEqual(temp);
  });

  it('should be parsed correctly from Json', () => {
    const obj = JSON.parse(witnessMessageJson);
    expect(WitnessMessage.fromJson(obj)).toBeJsonEqual(sampleWitnessMessage);
  });

  it('fromJson should throw an error if the Json has incorrect action', () => {
    const obj = {
      object: ObjectType.MESSAGE,
      action: ActionType.CREATE,
      message_id: mockMessageId,
      signature: mockSignature,
    };
    const createWrongObj = () => WitnessMessage.fromJson(obj);
    expect(createWrongObj).toThrow(ProtocolError);
  });

  it.skip('fromJson should throw an error if signature is incorrect', () => {
    const generateKeyPair = () => {
      const pair = sign.keyPair();
      return new KeyPair({
        publicKey: new PublicKey(Base64UrlData.fromBuffer(Buffer.from(pair.publicKey)).valueOf()),
        privateKey: new PrivateKey(Base64UrlData.fromBuffer(Buffer.from(pair.secretKey)).valueOf()),
      });
    };

    // incorrect signature
    expect(() => {
      WitnessMessage.fromJson({
        object: ObjectType.MESSAGE,
        action: ActionType.WITNESS,
        message_id: mockMessageId.valueOf(),
        signature: generateKeyPair().privateKey.sign(mockMessageId).valueOf(),
      });
    }).toThrow(ProtocolError);
  });

  it.skip('fromJson should throw an error if message_id is incorrect', () => {
    // inconsistent message_id
    expect(() => {
      WitnessMessage.fromJson({
        object: ObjectType.MESSAGE,
        action: ActionType.WITNESS,
        message_id: Base64UrlData.encode('inconsistent message_id').valueOf(),
        signature: mockSecretKey.sign(mockMessageId).valueOf(),
      });
    }).toThrow(ProtocolError);

    // inconsistent message_id for signature
    expect(() => {
      WitnessMessage.fromJson({
        object: ObjectType.MESSAGE,
        action: ActionType.WITNESS,
        message_id: mockMessageId.valueOf(),
        signature: mockSecretKey.sign(Base64UrlData.encode('inconsistent message_id')).valueOf(),
      });
    }).toThrow(ProtocolError);
  });

  describe('constructor', () => {
    it('should throw an error if message_id is undefined', () => {
      const createWrongObj = () =>
        new WitnessMessage({
          object: ObjectType.MESSAGE,
          action: ActionType.CREATE,
          signature: mockSignature,
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if signature is undefined', () => {
      const createWrongObj = () =>
        new WitnessMessage({
          object: ObjectType.MESSAGE,
          action: ActionType.CREATE,
          message_id: mockMessageId,
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });
  });
});
