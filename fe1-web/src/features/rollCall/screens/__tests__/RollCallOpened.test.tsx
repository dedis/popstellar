import React from 'react';
import { useRoute, useNavigation } from '@react-navigation/core';
import { fireEvent, render, waitFor } from '@testing-library/react-native';
import * as reactRedux from 'react-redux';
// @ts-ignore
import { fireScan as fakeQrReaderScan } from 'react-qr-reader';

import { Hash, PublicKey, Timestamp } from 'core/objects';
import STRINGS from 'resources/strings';
import keyPair from 'test_data/keypair.json';
import { mockLao, mockLaoId, mockLaoName, mockPopToken } from '__tests__/utils/TestUtils';

import * as token from 'features/wallet/objects/Token';

import { requestCloseRollCall as mockRequestCloseRollCall } from '../../network/RollCallMessageApi';
import RollCallOpened from '../RollCallOpened';

const mockPublicKey2 = new PublicKey(keyPair.publicKey2);

const TIMESTAMP = 1609455600;
const time = new Timestamp(TIMESTAMP).toString(); // 1st january 2021
const rollCallId = Hash.fromStringArray('R', mockLaoId, time, mockLaoName).toString();

jest.mock('@react-navigation/core');
jest.mock('react-qr-reader');
jest.mock('features/rollCall/network/RollCallMessageApi');

const mockToastShow = jest.fn();
const mockToastRet = {
  show: mockToastShow,
};
jest.mock('react-native-toast-notifications', () => ({
  useToast: () => mockToastRet,
}));

const generateTokenMock = jest.spyOn(token, 'generateToken');
let promise: Promise<any>;
beforeEach(() => {
  jest.resetAllMocks();

  const useSelectorMock = jest.spyOn(reactRedux, 'useSelector');
  useSelectorMock.mockReturnValue(mockLao);

  promise = Promise.resolve(mockPopToken);
  generateTokenMock.mockImplementation(() => promise);

  (useRoute as jest.Mock).mockReturnValue({
    name: STRINGS.roll_call_open,
    params: { rollCallID: rollCallId, time: time },
  });

  (useNavigation as jest.Mock).mockReturnValue({
    navigation: jest.fn(),
  });

  (mockRequestCloseRollCall as jest.Mock).mockImplementation(() => Promise.resolve());
});

describe('RollCallOpened', () => {
  it('renders correctly when no scan', async () => {
    const { toJSON } = render(<RollCallOpened />);
    await waitFor(() => {
      expect(generateTokenMock).toHaveBeenCalled();
      expect(toJSON()).toMatchSnapshot();
    });
  });

  it('renders correctly when scanning attendees', async () => {
    const { toJSON } = render(<RollCallOpened />);
    await waitFor(async () => {
      fakeQrReaderScan('123');
      fakeQrReaderScan('456');
      expect(generateTokenMock).toHaveBeenCalled();
    });
    expect(toJSON()).toMatchSnapshot();
  });

  it('shows toast when scanning attendees', async () => {
    render(<RollCallOpened />);
    await waitFor(async () => {
      fakeQrReaderScan('123');
      fakeQrReaderScan('456');
      expect(generateTokenMock).toHaveBeenCalled();
    });
    expect(mockToastShow).toHaveBeenCalledTimes(2);
  });

  it('shows toast when adding an attendee manually', async () => {
    const { getByText, getByPlaceholderText } = render(<RollCallOpened />);
    const addAttendeeButton = getByText(STRINGS.roll_call_add_attendee_manually);
    fireEvent.press(addAttendeeButton);
    const textInput = getByPlaceholderText(STRINGS.roll_call_attendee_token_placeholder);
    fireEvent.changeText(textInput, mockPublicKey2.valueOf());
    const confirmButton = getByText(STRINGS.general_add);
    fireEvent.press(confirmButton);
    await waitFor(() => {
      expect(mockToastShow).toHaveBeenCalledTimes(1);
    });
  });

  it('shows toast when trying to add an incorrect token manually', async () => {
    const { getByText, getByPlaceholderText } = render(<RollCallOpened />);
    const addAttendeeButton = getByText(STRINGS.roll_call_add_attendee_manually);
    fireEvent.press(addAttendeeButton);
    const textInput = getByPlaceholderText(STRINGS.roll_call_attendee_token_placeholder);
    fireEvent.changeText(textInput, 'data');
    const confirmButton = getByText(STRINGS.general_add);
    fireEvent.press(confirmButton);
    await waitFor(() => {
      expect(mockToastShow).toHaveBeenCalledTimes(1);
    });
  });

  it('closes correctly with no attendee', async () => {
    const button = render(<RollCallOpened />).getByText(STRINGS.roll_call_scan_close);
    await waitFor(() => expect(generateTokenMock).toHaveBeenCalled());
    fireEvent.press(button);
    expect(mockRequestCloseRollCall).toHaveBeenCalledWith(expect.anything(), [
      mockPopToken.publicKey,
    ]);
  });

  it('closes correctly with two attendees', async () => {
    const button = render(<RollCallOpened />).getByText(STRINGS.roll_call_scan_close);
    await waitFor(() => {
      fakeQrReaderScan('123');
      fakeQrReaderScan('456');
      expect(generateTokenMock).toHaveBeenCalled();
    });
    fireEvent.press(button);
    expect(mockRequestCloseRollCall).toHaveBeenCalledWith(expect.anything(), [
      new PublicKey('123'),
      new PublicKey('456'),
      mockPopToken.publicKey,
    ]);
  });
});
