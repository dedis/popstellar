import React from 'react';
import { act, render } from '@testing-library/react-native';
import {
  EventTags,
  Hash, PublicKey, Timestamp,
} from 'model/objects';
import keyPair from 'test_data/keypair.json';
import RollCallOpened from '../RollCallOpened'; // Don't change the order, otherwise the tests will fail
import { fireScan as fakeQrReaderScan } from '__mocks__/react-qr-reader';
import { requestCloseRollCall as mockRequestCloseRollCall } from '__mocks__/MessageApi';
import STRINGS from 'res/strings';
import { useRoute } from '@react-navigation/core';

jest.mock('@react-navigation/core');

export const mockPublicKey = new PublicKey(keyPair.publicKey);

const org = mockPublicKey;
const time = new Timestamp(1609455600).toString(); // 1st january 2021
const name = 'mock Lao';
const mockLaoIdHash: Hash = Hash.fromStringArray(org.toString(), time, name);
const mockLaoId: string = mockLaoIdHash.toString();
const rollCallId = Hash.fromStringArray('R', mockLaoId, time, name).toString();

jest.mock('react-qr-reader');

const mockToastShow = jest.fn();
jest.mock('react-native-toast-notifications', () => ({
  useToast: () => ({
    show: mockToastShow,
  }),
}));

const mockNavigate = jest.fn();
jest.mock('@react-navigation/native', () => {
  const actualNav = jest.requireActual('@react-navigation/native');
  return {
    ...actualNav,
    useNavigation: () => ({
      navigate: mockNavigate,
    }),
  };
});

describe('RollCallOpened', () => {
  it('renders correctly when no scan', async () => {
    (useRoute as jest.Mock).mockReturnValue({
      name: STRINGS.roll_call_open,
      params: { rollCallId: rollCallId, time: time },
    });
    const { toJSON } = render(
      <RollCallOpened />,
    );
    expect(toJSON()).toMatchSnapshot();
  });

  it('can scan one attendee', async () => {
    (useRoute as jest.Mock).mockReturnValue({
      name: STRINGS.roll_call_open,
      params: { rollCallId: rollCallId, time: time },
    });
    render(
      <RollCallOpened />,
    );
    act(() => {
      fakeQrReaderScan('123');
    });
    expect(mockToastShow).toHaveBeenCalledTimes(1);
  });

  it('close correctly', async () => {
    (useRoute as jest.Mock).mockReturnValue({
      name: STRINGS.roll_call_open,
      params: { rollCallId: rollCallId, time: time },
    });
    render(
      <RollCallOpened />,
    );
    mockRequestCloseRollCall()
      .then(() => {
        expect(mockNavigate).toBeCalledTimes(1);
      });
  });
});
