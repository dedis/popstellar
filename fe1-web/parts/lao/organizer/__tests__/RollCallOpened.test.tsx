import React from 'react';
import { act, render } from '@testing-library/react-native';
import {
  Hash, PrivateKey, PublicKey, Timestamp,
} from 'model/objects';
import keyPair from 'test_data/keypair.json';
import RollCallOpened from '../RollCallOpened'; // Don't change the order, otherwise the tests will fail
import { fireScan as fakeQrReaderScan } from '__mocks__/react-qr-reader';
import STRINGS from 'res/strings';
import { useRoute } from '@react-navigation/core';

jest.mock('@react-navigation/core');

export const mockPublicKey = new PublicKey(keyPair.publicKey);
export const mockSecretKey = new PrivateKey(keyPair.privateKey);

const org = mockPublicKey;
const time = new Timestamp(1609455600); // 1st january 2021
const name = 'lao for testng roll-call';
const mockLaoId: Hash = Hash.fromStringArray(org.toString(), time.toString(), name);
const rollCallId = Hash.fromStringArray('R', mockLaoId.toString(), time.toString(), name.toString());

jest.mock('react-qr-reader');

const mockToastShow = jest.fn();
jest.mock('react-native-toast-notifications', () => ({
  useToast: () => ({
    show: mockToastShow,
  }),
}));

describe('RollCallOpened', () => {
  it('renders correctly when no scan', async () => {
    // (useRoute as jest.Mocked<typeof useRoute>).mockReturnValue({
    (useRoute as jest.Mock).mockReturnValue({
      name: STRINGS.roll_call_open,
      params: { rollCallId: rollCallId.toString(), time: time.toString() },
    });
    const { toJSON } = render(
      <RollCallOpened />,
    );
    expect(toJSON()).toMatchSnapshot();
  });

  it('can scan one attendee', async () => {
    (useRoute as jest.Mock).mockReturnValue({
      name: STRINGS.roll_call_open,
      params: { rollCallId: rollCallId.toString(), time: time.toString() },
    });
    render(
      <RollCallOpened />,
    );
    act(() => {
      fakeQrReaderScan('123');
    });
    expect(mockToastShow).toHaveBeenCalledTimes(1);
  });
});
