import React from 'react';
import { useRoute } from '@react-navigation/core';
import {
  act, fireEvent, render, waitFor,
} from '@testing-library/react-native';
import {
  Hash, Lao, LaoState, PopToken, PrivateKey, PublicKey, Timestamp,
} from 'model/objects';
import STRINGS from 'res/strings';
import keyPair from 'test_data/keypair.json';
import { requestCloseRollCall as mockRequestCloseRollCall } from 'network/MessageApi';
import { OpenedLaoStore } from 'store';
import * as reactRedux from 'react-redux';
// @ts-ignore
import { fireScan as fakeQrReaderScan } from 'react-qr-reader';
import RollCallOpened from '../RollCallOpened';

export const mockPublicKey = new PublicKey(keyPair.publicKey);

const org = mockPublicKey;
const TIMESTAMP = 1609455600;
const time = new Timestamp(TIMESTAMP).toString(); // 1st january 2021
const name = 'MyLao';
const mockLaoIdHash: Hash = Hash.fromStringArray(org.toString(), time, name);
const mockLaoId: string = mockLaoIdHash.toString();
const rollCallId = Hash.fromStringArray('R', mockLaoId, time, name).toString();
const laoState: LaoState = {
  id: mockLaoId,
  name: 'MyLao',
  creation: TIMESTAMP,
  last_modified: TIMESTAMP,
  organizer: 'organizerPublicKey',
  witnesses: [],
};
const mockLao = Lao.fromState(laoState);

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

const mockTokenPublicKey = new PublicKey('mockTokenPublicKey');
const mockTokenPrivateKey = new PrivateKey('mockTokenPrivateKey');
const mockPopToken = new PopToken({
  publicKey: mockTokenPublicKey,
  privateKey: mockTokenPrivateKey,
});
jest.mock('model/objects/wallet/Token.ts', () => ({
  generateToken: jest.fn(() => Promise.resolve(mockPopToken)),
}));

describe('RollCallOpened', () => {
  const useSelectorMock = jest.spyOn(reactRedux, 'useSelector');
  useSelectorMock.mockReturnValue({ mockLao });

  it('renders correctly when no scan', async () => {
    (useRoute as jest.Mock).mockReturnValue({
      name: STRINGS.roll_call_open,
      params: { rollCallID: rollCallId, time: time },
    });
    const { toJSON } = render(
      <RollCallOpened />,
    );
    await waitFor(() => {
      expect(toJSON()).toMatchSnapshot();
    });
  });

  it('can scan attendees', async () => {
    (useRoute as jest.Mock).mockReturnValue({
      name: STRINGS.roll_call_open,
      params: { rollCallID: rollCallId, time: time },
    });
    render(
      <RollCallOpened />,
    );
    act(() => {
      fakeQrReaderScan('123');
      fakeQrReaderScan('456');
    });
    await waitFor(() => {
      expect(mockToastShow).toHaveBeenCalledTimes(2);
    });
  });

  it('close correctly with no attendee', async () => {
    (useRoute as jest.Mock).mockReturnValue({
      name: STRINGS.roll_call_open,
      params: { rollCallID: rollCallId, time: time },
    });
    const getMock = jest.spyOn(OpenedLaoStore, 'get');
    getMock.mockImplementation(() => Lao.fromState(laoState));
    const button = render(
      <RollCallOpened />,
    ).getByText(STRINGS.roll_call_scan_close);
    await waitFor(() => {
      fireEvent.press(button);
      expect(mockRequestCloseRollCall).toHaveBeenCalledWith(expect.anything(),
        [mockTokenPublicKey]);
    });
  });

  it('close correctly with two attendees', async () => {
    (useRoute as jest.Mock).mockReturnValue({
      name: STRINGS.roll_call_open,
      params: { rollCallID: rollCallId, time: time },
    });
    const getMock = jest.spyOn(OpenedLaoStore, 'get');
    getMock.mockImplementation(() => Lao.fromState(laoState));
    const button = render(
      <RollCallOpened />,
    ).getByText(STRINGS.roll_call_scan_close);
    await waitFor(() => {
      act(() => {
        fakeQrReaderScan('123');
        fakeQrReaderScan('456');
      });
      fireEvent.press(button);
      expect(mockRequestCloseRollCall).toHaveBeenCalledWith(expect.anything(),
        [new PublicKey('123'), new PublicKey('456'), mockTokenPublicKey]);
    });
  });
});
