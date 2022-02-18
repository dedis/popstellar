import React from 'react';
import { useRoute } from '@react-navigation/core';
import { act, fireEvent, render, waitFor } from '@testing-library/react-native';
import * as reactRedux from 'react-redux';
// @ts-ignore
import { fireScan as fakeQrReaderScan } from 'react-qr-reader';

import { Hash, PublicKey, Timestamp } from 'core/objects';
import STRINGS from 'resources/strings';
import keyPair from 'test_data/keypair.json';
import { Lao } from 'features/lao/objects';
import { OpenedLaoStore } from 'features/lao/store';
import {
  mockLao,
  mockLaoId,
  mockLaoName,
  mockLaoState,
  mockPopToken,
} from '__tests__/utils/TestUtils';

import { requestCloseRollCall as mockRequestCloseRollCall } from '../../network/RollCallMessageApi';
import RollCallOpened from '../RollCallOpened';

const mockPublicKey2 = new PublicKey(keyPair.publicKey2);

const TIMESTAMP = 1609455600;
const time = new Timestamp(TIMESTAMP).toString(); // 1st january 2021
const rollCallId = Hash.fromStringArray('R', mockLaoId, time, mockLaoName).toString();

jest.mock('@react-navigation/core');
jest.mock('react-qr-reader');
jest.mock('features/rollCall/network/RollCallMessageApi.ts');

let mockToastShow = jest.fn();
jest.mock('react-native-toast-notifications', () => ({
  useToast: () => ({
    show: mockToastShow,
  }),
}));

jest.mock('features/wallet/objects/Token.ts', () => ({
  generateToken: jest.fn(() => Promise.resolve(mockPopToken)),
}));

beforeEach(() => {
  mockToastShow = jest.fn();
});

describe('RollCallOpened', () => {
  const useSelectorMock = jest.spyOn(reactRedux, 'useSelector');
  useSelectorMock.mockReturnValue({ mockLao });

  (useRoute as jest.Mock).mockReturnValue({
    name: STRINGS.roll_call_open,
    params: { rollCallID: rollCallId, time: time },
  });

  it('renders correctly when no scan', async () => {
    const { toJSON } = render(<RollCallOpened />);
    await waitFor(() => {
      expect(toJSON()).toMatchSnapshot();
    });
  });

  it('shows toast when scanning attendees', async () => {
    render(<RollCallOpened />);
    act(() => {
      fakeQrReaderScan('123');
      fakeQrReaderScan('456');
    });
    await waitFor(() => {
      expect(mockToastShow).toHaveBeenCalledTimes(2);
    });
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
    const getMock = jest.spyOn(OpenedLaoStore, 'get');
    getMock.mockImplementation(() => Lao.fromState(mockLaoState));
    const button = render(<RollCallOpened />).getByText(STRINGS.roll_call_scan_close);
    await waitFor(() => {
      fireEvent.press(button);
      expect(mockRequestCloseRollCall).toHaveBeenCalledWith(expect.anything(), [
        mockPopToken.publicKey,
      ]);
    });
  });

  it('closes correctly with two attendees', async () => {
    const getMock = jest.spyOn(OpenedLaoStore, 'get');
    getMock.mockImplementation(() => Lao.fromState(mockLaoState));
    const button = render(<RollCallOpened />).getByText(STRINGS.roll_call_scan_close);
    await waitFor(() => {
      act(() => {
        fakeQrReaderScan('123');
        fakeQrReaderScan('456');
      });
      fireEvent.press(button);
      expect(mockRequestCloseRollCall).toHaveBeenCalledWith(expect.anything(), [
        new PublicKey('123'),
        new PublicKey('456'),
        mockPopToken.publicKey,
      ]);
    });
  });
});
