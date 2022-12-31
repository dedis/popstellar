import { describe } from '@jest/globals';

import { mockChannel, mockLao, mockLaoId, mockPopToken } from '__tests__/utils';
import { addMessageWitnessSignature } from 'core/network/ingestion';
import { ActionType, ObjectType } from 'core/network/jsonrpc/messages';
import { Base64UrlData, Hash, KeyPair, Signature, Timestamp, WitnessSignature } from 'core/objects';
import { dispatch } from 'core/redux';
import { WitnessFeature } from 'features/witness/interface';

import { WitnessMessage } from '../messages';
import { handleWitnessMessage } from '../WitnessHandler';

// mock dispatch function

jest.mock('core/redux', () => {
  const actualModule = jest.requireActual('core/redux');
  return {
    ...actualModule,
    dispatch: jest.fn(() => {}),
  };
});

const witnessKeyPair: KeyPair = mockPopToken;

const getMockLaoWithoutWitnesses = (): WitnessFeature.Lao => ({
  ...mockLao,
  witnesses: [],
});

const getMockLaoWithWitness = (): WitnessFeature.Lao => ({
  ...mockLao,
  witnesses: [witnessKeyPair.publicKey],
});

const TIMESTAMP = new Timestamp(1609455600); // 1st january 2021

const mockMessageData = {
  receivedAt: TIMESTAMP,
  receivedFrom: 'some address',
  laoId: mockLaoId,
  data: Base64UrlData.encode('some data'),
  sender: witnessKeyPair.publicKey,
  signature: Base64UrlData.encode('some data') as Signature,
  channel: mockChannel,
  message_id: new Hash('some string'),
  witness_signatures: [],
};

afterEach(() => {
  jest.clearAllMocks();
});

describe('WitnessHandler', () => {
  describe('handleWitnessMessage', () => {
    it('returns false on a wrong message object', () => {
      expect(
        handleWitnessMessage(getMockLaoWithoutWitnesses)({
          ...mockMessageData,
          messageData: {
            object: ObjectType.CHIRP,
            action: ActionType.WITNESS,
          },
        }),
      ).toBeFalse();
    });

    it('returns false on a wrong message action', () => {
      expect(
        handleWitnessMessage(getMockLaoWithoutWitnesses)({
          ...mockMessageData,
          messageData: {
            object: ObjectType.MESSAGE,
            action: ActionType.CREATE,
          },
        }),
      ).toBeFalse();
    });

    it('returns false if the message lao id does not match the current lao id', () => {
      expect(
        handleWitnessMessage(getMockLaoWithoutWitnesses)({
          ...mockMessageData,
          laoId: new Hash('some bogus lao'),
          messageData: {
            object: ObjectType.CHIRP,
            action: ActionType.WITNESS,
          },
        }),
      ).toBeFalse();
    });

    it('returns false if the sender is not a witness of the current lao', () => {
      expect(
        handleWitnessMessage(getMockLaoWithoutWitnesses)({
          ...mockMessageData,
          messageData: {
            object: ObjectType.MESSAGE,
            action: ActionType.WITNESS,
          },
        }),
      ).toBeFalse();
    });

    it('returns false if the received signature is invalid', () => {
      expect(
        handleWitnessMessage(getMockLaoWithWitness)({
          ...mockMessageData,
          messageData: {
            object: ObjectType.MESSAGE,
            action: ActionType.WITNESS,
            message_id: new Hash('some message id'),
            signature: new Signature('some bogus signature'),
          } as WitnessMessage,
        }),
      ).toBeFalse();
    });

    it('can process a valid message', () => {
      const mockMessageId = new Hash('some message id');
      const mockSignature: Signature = witnessKeyPair.privateKey.sign(mockMessageId);

      expect(
        handleWitnessMessage(getMockLaoWithWitness)({
          ...mockMessageData,
          messageData: {
            object: ObjectType.MESSAGE,
            action: ActionType.WITNESS,
            message_id: mockMessageId,
            signature: mockSignature,
          } as WitnessMessage,
        }),
      ).toBeTrue();

      // ensure addMessageWitnessSignature action has been dispatched
      expect(dispatch).toHaveBeenCalledWith(
        addMessageWitnessSignature(
          mockMessageId,
          new WitnessSignature({
            witness: witnessKeyPair.publicKey,
            signature: mockSignature,
          }).toState(),
        ),
      );
      expect(dispatch).toHaveBeenCalledTimes(1);
    });
  });
});
