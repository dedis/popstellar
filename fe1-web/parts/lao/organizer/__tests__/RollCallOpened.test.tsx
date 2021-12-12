import React from 'react';
import { useRoute } from '@react-navigation/core';
import { act, fireEvent, render } from '@testing-library/react-native';
import {
  Hash, Lao, LaoState, PublicKey, Timestamp,
} from 'model/objects';
import STRINGS from 'res/strings';
import keyPair from 'test_data/keypair.json';
import { requestCloseRollCall as mockRequestCloseRollCall } from 'network/MessageApi';
import { OpenedLaoStore } from 'store';
// @ts-ignore
import { fireScan as fakeQrReaderScan } from 'react-qr-reader';
import RollCallOpened from '../RollCallOpened';

const org = new PublicKey(keyPair.publicKey);
const TIMESTAMP = 1609455600;
const time = new Timestamp(TIMESTAMP).toString(); // 1st january 2021
const name = 'MyLao';
const mockLaoIdHash: Hash = Hash.fromStringArray(org.toString(), time, name);
const mockLaoId: string = mockLaoIdHash.toString();
const rollCallId = Hash.fromStringArray('R', mockLaoId, time, name).toString();
const laoState: LaoState = {
  id: '1234',
  name: 'MyLao',
  creation: TIMESTAMP,
  last_modified: TIMESTAMP,
  organizer: '1234',
  witnesses: [],
};

jest.mock('@react-navigation/core');
jest.mock('react-qr-reader');
jest.mock('network/MessageApi');

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

  it('can scan attendees', async () => {
    (useRoute as jest.Mock).mockReturnValue({
      name: STRINGS.roll_call_open,
      params: { rollCallId: rollCallId, time: time },
    });
    render(
      <RollCallOpened />,
    );
    act(() => {
      fakeQrReaderScan('123');
      fakeQrReaderScan('456');
    });
    expect(mockToastShow).toHaveBeenCalledTimes(2);
  });

  it('close correctly with no attendee', async () => {
    (useRoute as jest.Mock).mockReturnValue({
      name: STRINGS.roll_call_open,
      params: { rollCallId: rollCallId, time: time },
    });
    const getMock = jest.spyOn(OpenedLaoStore, 'get');
    getMock.mockImplementation(() => Lao.fromState(laoState));
    const button = render(
      <RollCallOpened />,
    ).getByText(STRINGS.roll_call_scan_close);
    fireEvent.press(button);
    expect(mockRequestCloseRollCall).toHaveBeenCalledWith(expect.anything(), []);
  });

  it('close correctly with two attendees', async () => {
    (useRoute as jest.Mock).mockReturnValue({
      name: STRINGS.roll_call_open,
      params: { rollCallId: rollCallId, time: time },
    });
    const getMock = jest.spyOn(OpenedLaoStore, 'get');
    getMock.mockImplementation(() => Lao.fromState(laoState));
    const button = render(
      <RollCallOpened />,
    ).getByText(STRINGS.roll_call_scan_close);
    act(() => {
      fakeQrReaderScan('123');
      fakeQrReaderScan('456');
    });
    fireEvent.press(button);
    expect(mockRequestCloseRollCall).toHaveBeenCalledWith(expect.anything(), [new PublicKey('123'), new PublicKey('456')]);
  });
});
