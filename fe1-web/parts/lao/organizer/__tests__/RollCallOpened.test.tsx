import React from 'react';
import { render } from '@testing-library/react-native';
import {
  Base64UrlData, Hash, PrivateKey, PublicKey, Timestamp,
} from 'model/objects';
import keyPair from 'test_data/keypair.json';
import RollCallOpened from '../RollCallOpened';
import QrReader from 'react-qr-reader';
import STRINGS from "res/strings";
import { useToast } from 'react-native-toast-notifications';
import { useRoute } from '@react-navigation/core';

jest.mock('@react-navigation/core');

export const mockPublicKey = new PublicKey(keyPair.publicKey);
export const mockSecretKey = new PrivateKey(keyPair.privateKey);

const org = mockPublicKey;
const time = new Timestamp(1609455600); // 1st january 2021
const name = 'lao for testng roll-call';
const location = 'EPFL';
const mockLaoId: Hash = Hash.fromStringArray(org.toString(), time.toString(), name);
const rollCallId = Hash.fromStringArray('R', mockLaoId.toString(), time.toString(), name.toString());

jest.mock('react-qr-reader', () => function onScan() {
  /* const mockToast = useToast();
  return (
    mockToast.show('Scanned', {
      type: 'success',
      placement: 'top',
      duration: 4000,
    })
  ); */
  console.log('participants + 1');
});

// beforeEach(() => {
//   useRoute = jest.fn();
// });

// const useRoute = jest.spyOn(require('@react-navigation/core'), 'useRoute');

describe('RollCallOpened', () => {
  it('renders correctly when no scan', async () => {
    // (useRoute as jest.Mocked<typeof useRoute>).mockReturnValue({
    (useRoute as jest.Mock).mockReturnValue({
    // useRoute.mockReturnValue({
      name: STRINGS.roll_call_open,
      params: { rollCallId: rollCallId.toString(), time: time.toString() },
    });
    const { toJSON } = render(
      <RollCallOpened />,
    );
    expect(toJSON()).toMatchSnapshot();
  });
});
