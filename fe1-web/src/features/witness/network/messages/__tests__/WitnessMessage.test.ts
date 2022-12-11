import 'jest-extended';
import '__tests__/utils/matchers';

import { mockKeyPair, mockPopToken } from '__tests__/utils';
import { ActionType, ObjectType } from 'core/network/jsonrpc/messages';
import { Hash, ProtocolError, Signature } from 'core/objects';

import { WitnessMessage } from '../WitnessMessage';

const mockMessageId = new Hash('message_id');
const mockSignature = mockKeyPair.privateKey.sign(mockMessageId);

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

  it('fromJson should throw an error if signature does not match message_id', () => {
    // precondition
    expect(mockKeyPair.privateKey.valueOf() !== mockPopToken.privateKey.valueOf()).toBeTrue();

    // incorrect signature
    expect(() => {
      WitnessMessage.fromJson({
        object: ObjectType.MESSAGE,
        action: ActionType.WITNESS,
        message_id: mockMessageId.valueOf(),
        signature: new Signature('some invalid signature'),
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
