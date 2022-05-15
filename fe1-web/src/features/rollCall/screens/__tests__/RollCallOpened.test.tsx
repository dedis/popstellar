import { useNavigation, useRoute } from '@react-navigation/core';
import { fireEvent, render, waitFor } from '@testing-library/react-native';
import React from 'react';
// @ts-ignore
import { fireScan as fakeQrReaderScan } from 'react-qr-reader';
import * as reactRedux from 'react-redux';

import { mockLao, mockLaoId, mockLaoName, mockPopToken } from '__tests__/utils/TestUtils';
import { Hash, PublicKey, Timestamp } from 'core/objects';
import * as token from 'features/wallet/objects/Token';
import STRINGS from 'resources/strings';

import { requestCloseRollCall as mockRequestCloseRollCall } from '../../network/RollCallMessageApi';
import RollCallOpened from '../RollCallOpened';

const mockPublicKey2 = new PublicKey('mockPublicKey2_fFcHDaVHcCcY8IBfHE7auXJ7h4ms=');
const mockPublicKey3 = new PublicKey('mockPublicKey3_fFcHDaVHcCcY8IBfHE7auXJ7h4ms=');

const TIMESTAMP = 1609455600;
const time = new Timestamp(TIMESTAMP).toString(); // 1st january 2021
const rollCallId = Hash.fromStringArray('R', mockLaoId, time, mockLaoName).toString();
const mockRollCall = {
  id: rollCallId,
  idAlias: rollCallId,
};

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
  useSelectorMock.mockReturnValueOnce(mockLao).mockReturnValue(mockRollCall);

  promise = Promise.resolve(mockPopToken);
  generateTokenMock.mockImplementation(() => promise);

  (useRoute as jest.Mock).mockReturnValue({
    name: STRINGS.roll_call_open,
    params: { rollCallID: rollCallId },
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
      // scan valid pop tokens
      fakeQrReaderScan(mockPublicKey2.valueOf());
      fakeQrReaderScan(mockPublicKey3.valueOf());
      // scan invalid pop tokens
      fakeQrReaderScan('123');
      fakeQrReaderScan('456');
      expect(generateTokenMock).toHaveBeenCalled();
    });
    expect(toJSON()).toMatchSnapshot();
  });

  it('shows toast when scanning attendees', async () => {
    render(<RollCallOpened />);
    await waitFor(async () => {
      fakeQrReaderScan(mockPublicKey2.valueOf());
      fakeQrReaderScan(mockPublicKey3.valueOf());
      expect(generateTokenMock).toHaveBeenCalled();
    });
    expect(mockToastShow).toHaveBeenCalledTimes(2);
  });

  it('shows toast when adding an attendee manually', async () => {
    const { getByText, getByPlaceholderText } = render(<RollCallOpened />);
    const addAttendeeButton = getByText(STRINGS.roll_call_add_attendee_manually);
    fireEvent.press(addAttendeeButton);
    const textInput = getByPlaceholderText(STRINGS.roll_call_attendee_token_placeholder);
    fireEvent.changeText(textInput, mockPopToken.publicKey.valueOf());
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
      fakeQrReaderScan(mockPublicKey2.valueOf());
      fakeQrReaderScan(mockPublicKey3.valueOf());
      expect(generateTokenMock).toHaveBeenCalled();
    });
    fireEvent.press(button);
    expect(mockRequestCloseRollCall).toHaveBeenCalledWith(expect.anything(), [
      mockPublicKey2,
      mockPublicKey3,
      mockPopToken.publicKey,
    ]);
  });
});
