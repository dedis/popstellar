import { fireEvent, render, waitFor } from '@testing-library/react-native';
import React from 'react';
// @ts-ignore
import { fireScan as fakeQrReaderScan } from 'react-qr-reader';
import { Provider } from 'react-redux';
import { combineReducers, createStore } from 'redux';

import MockNavigator from '__tests__/components/MockNavigator';
import { mockLao, mockLaoIdHash, mockPopToken } from '__tests__/utils/TestUtils';
import FeatureContext from 'core/contexts/FeatureContext';
import { PublicKey } from 'core/objects';
import { eventReducer, makeEventByTypeSelector } from 'features/events/reducer';
import { connectToLao, laoReducer } from 'features/lao/reducer';
import {
  mockRollCallWithAlias,
  mockRollCallWithAliasState,
} from 'features/rollCall/__tests__/utils';
import { RollCallReactContext, ROLLCALL_FEATURE_IDENTIFIER } from 'features/rollCall/interface';
import { RollCall } from 'features/rollCall/objects';
import { addRollCall, rollCallReducer } from 'features/rollCall/reducer';
import { getWalletState, walletReducer } from 'features/wallet/reducer';
import STRINGS from 'resources/strings';

import { requestCloseRollCall as mockRequestCloseRollCall } from '../../network/RollCallMessageApi';
import RollCallOpened from '../RollCallOpened';

const mockPublicKey2 = new PublicKey('mockPublicKey2_fFcHDaVHcCcY8IBfHE7auXJ7h4ms=');
const mockPublicKey3 = new PublicKey('mockPublicKey3_fFcHDaVHcCcY8IBfHE7auXJ7h4ms=');

jest.mock('@react-navigation/core', () => {
  const actualNavigation = jest.requireActual('@react-navigation/core');
  return {
    ...actualNavigation,
    useNavigation: () => ({
      navigate: () => {},
    }),
  };
});
jest.mock('react-qr-reader');
jest.mock('features/rollCall/network/RollCallMessageApi');

const mockToastShow = jest.fn();
const mockToastRet = {
  show: mockToastShow,
};
jest.mock('react-native-toast-notifications', () => ({
  useToast: () => mockToastRet,
}));

(mockRequestCloseRollCall as jest.Mock).mockImplementation(() => Promise.resolve());

const mockRollCall = RollCall.fromState({ ...mockRollCallWithAliasState, attendees: [] });
const rollCallID = mockRollCallWithAlias.id.valueOf();

// set up mock store
const mockStore = createStore(
  combineReducers({ ...laoReducer, ...eventReducer, ...rollCallReducer, ...walletReducer }),
);
mockStore.dispatch(connectToLao(mockLao.toState()));
mockStore.dispatch(addRollCall(mockRollCall.toState()));

const mockGenerateToken = jest.fn(() => Promise.resolve(mockPopToken));

const contextValue = {
  [ROLLCALL_FEATURE_IDENTIFIER]: {
    useCurrentLaoId: () => mockLaoIdHash,
    makeEventByTypeSelector: makeEventByTypeSelector,
    generateToken: mockGenerateToken,
    hasSeed: () => getWalletState(mockStore.getState()).seed !== undefined,
  } as RollCallReactContext,
};

const renderRollCallOpened = () =>
  render(
    <Provider store={mockStore}>
      <FeatureContext.Provider value={contextValue}>
        <MockNavigator component={RollCallOpened} params={{ rollCallID }} />
      </FeatureContext.Provider>
    </Provider>,
  );

beforeEach(() => {
  jest.clearAllMocks();
});

describe('RollCallOpened', () => {
  it('renders correctly when no scan', async () => {
    const { toJSON } = renderRollCallOpened();

    await waitFor(() => {
      expect(mockGenerateToken).toHaveBeenCalled();
      expect(toJSON()).toMatchSnapshot();
    });
  });

  it('renders correctly when scanning attendees', async () => {
    const { toJSON } = renderRollCallOpened();

    await waitFor(async () => {
      // scan valid pop tokens
      fakeQrReaderScan(mockPublicKey2.valueOf());
      fakeQrReaderScan(mockPublicKey3.valueOf());
      // scan invalid pop tokens
      fakeQrReaderScan('123');
      fakeQrReaderScan('456');
      expect(mockGenerateToken).toHaveBeenCalled();
    });

    expect(toJSON()).toMatchSnapshot();
  });

  it('shows toast when scanning attendees', async () => {
    renderRollCallOpened();

    await waitFor(async () => {
      fakeQrReaderScan(mockPublicKey2.valueOf());
      fakeQrReaderScan(mockPublicKey3.valueOf());
      expect(mockGenerateToken).toHaveBeenCalled();
    });
    expect(mockToastShow).toHaveBeenCalledTimes(2);
  });

  it('shows toast when adding an attendee manually', async () => {
    const { getByText, getByPlaceholderText } = renderRollCallOpened();

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
    const { getByText, getByPlaceholderText } = renderRollCallOpened();

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
    const button = renderRollCallOpened().getByText(STRINGS.roll_call_scan_close);

    await waitFor(() => {
      expect(mockGenerateToken).toHaveBeenCalled();
    });

    fireEvent.press(button);

    expect(mockRequestCloseRollCall).toHaveBeenCalledWith(mockLaoIdHash, expect.anything(), [
      mockPopToken.publicKey,
    ]);
  });

  it('closes correctly with two attendees', async () => {
    const button = renderRollCallOpened().getByText(STRINGS.roll_call_scan_close);
    await waitFor(() => {
      fakeQrReaderScan(mockPublicKey2.valueOf());
      fakeQrReaderScan(mockPublicKey3.valueOf());
      expect(mockGenerateToken).toHaveBeenCalled();
    });
    fireEvent.press(button);
    expect(mockRequestCloseRollCall).toHaveBeenCalledWith(mockLaoIdHash, expect.anything(), [
      mockPublicKey2,
      mockPublicKey3,
      mockPopToken.publicKey,
    ]);
  });
});
