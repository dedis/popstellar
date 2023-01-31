import 'jest-extended';
import '__tests__/utils/matchers';

import { mockChannel, mockKeyPair } from '__tests__/utils/TestUtils';
import { KeyPairStore } from 'core/keypair';
import { publish } from 'core/network/JsonRpcApi';
import { Hash } from 'core/objects';

import { WitnessMessage } from '../messages';
import * as WitnessMessageApi from '../WitnessMessageApi';

jest.mock('core/network/JsonRpcApi');
const mockPublish = publish as jest.Mock;

jest.mock('core/keypair/KeyPairStore');

beforeAll(() => {
  KeyPairStore.store(mockKeyPair);
});

describe('WitnessMessageApi', () => {
  it('should create the correct request for requestWitnessMessage', async () => {
    const mockMessageId = new Hash('some message id');

    await WitnessMessageApi.requestWitnessMessage(mockChannel, mockMessageId);

    expect(mockPublish).toHaveBeenCalledWith(
      mockChannel,
      new WitnessMessage({
        message_id: mockMessageId,
        signature: mockKeyPair.privateKey.sign(mockMessageId),
      }),
    );
    expect(mockPublish).toHaveBeenCalledTimes(1);
  });
});
