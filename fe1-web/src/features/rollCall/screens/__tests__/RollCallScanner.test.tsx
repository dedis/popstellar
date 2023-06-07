import { useNavigation } from '@react-navigation/core';
import { configureStore } from '@reduxjs/toolkit';
import { fireEvent, render, waitFor } from '@testing-library/react-native';
// @ts-ignore
import { fireScan as fakeQrReaderScan } from 'expo-camera';
import React from 'react';
import { Provider } from 'react-redux';
import { act } from 'react-test-renderer';
import { combineReducers } from 'redux';

import MockNavigator from '__tests__/components/MockNavigator';
import { mockLao, mockLaoId, mockPopToken } from '__tests__/utils/TestUtils';
import FeatureContext from 'core/contexts/FeatureContext';
import { PublicKey } from 'core/objects';
import { ScannablePopToken } from 'core/objects/ScannablePopToken';
import { eventReducer, makeEventByTypeSelector } from 'features/events/reducer';
import { laoReducer, setCurrentLao } from 'features/lao/reducer';
import {
  mockRollCallWithAlias,
  mockRollCallWithAliasState,
} from 'features/rollCall/__tests__/utils';
import { ROLLCALL_FEATURE_IDENTIFIER, RollCallReactContext } from 'features/rollCall/interface';
import { RollCall } from 'features/rollCall/objects';
import { addRollCall, rollCallReducer } from 'features/rollCall/reducer';
import { getWalletState, walletReducer } from 'features/wallet/reducer';

import { requestCloseRollCall as mockRequestCloseRollCall } from '../../network/RollCallMessageApi';
import RollCallScanner, { RollCallOpenedHeaderLeft } from '../RollCallScanner';

const mockPublicKey2 = new PublicKey('mockPublicKey2_fFcHDaVHcCcY8IBfHE7auXJ7h4ms=');
const mockPublicKey3 = new PublicKey('mockPublicKey3_fFcHDaVHcCcY8IBfHE7auXJ7h4ms=');
const mockOrganizerPublicKey = new PublicKey('mockOrganizerPublicKey_fFcHDaVHcCcY8IBfHE7a=');

jest.mock('@react-navigation/core', () => {
  const actualNavigation = jest.requireActual('@react-navigation/core');

  const navigate = jest.fn();
  const addListener = jest.fn();
  const setParams = jest.fn();

  return {
    ...actualNavigation,
    useNavigation: () => ({
      navigate,
      addListener,
      setParams,
    }),
  };
});
jest.mock('expo-camera');
jest.mock('features/rollCall/network/RollCallMessageApi');

// just a mock hook
// eslint-disable-next-line react-hooks/rules-of-hooks
const { navigate, addListener, setParams } = useNavigation();
const mockToastShow = jest.fn();
const mockToastRet = {
  show: mockToastShow,
};
jest.mock('react-native-toast-notifications', () => ({
  useToast: () => mockToastRet,
}));

(mockRequestCloseRollCall as jest.Mock).mockImplementation(() => Promise.resolve());

const mockRollCall = RollCall.fromState({
  ...mockRollCallWithAliasState,
  attendees: [],
});
const rollCallId = mockRollCallWithAlias.id.valueOf();

// set up mock store
const mockStore = configureStore({
  reducer: combineReducers({
    ...laoReducer,
    ...eventReducer,
    ...rollCallReducer,
    ...walletReducer,
  }),
});
mockStore.dispatch(setCurrentLao(mockLao));
mockStore.dispatch(addRollCall(mockRollCall.toState()));

const contextValue = {
  [ROLLCALL_FEATURE_IDENTIFIER]: {
    useCurrentLaoId: () => mockLaoId,
    useConnectedToLao: () => true,
    makeEventByTypeSelector: makeEventByTypeSelector,
    generateToken: jest.fn(),
    hasSeed: () => getWalletState(mockStore.getState()).seed !== undefined,
  } as RollCallReactContext,
};

const didFocus = () =>
  // call focus event listener
  (addListener as jest.Mock).mock.calls
    .filter(([eventName]) => eventName === 'focus')
    .forEach((args) => args[1]());

const renderRollCallOpened = (
  mockAttendeePopTokens: string[] = [mockOrganizerPublicKey.toString()],
) => {
  const renderedRollCallOpened = render(
    <Provider store={mockStore}>
      <FeatureContext.Provider value={contextValue}>
        <MockNavigator
          component={RollCallScanner}
          params={{
            rollCallId,
            attendeePopTokens: mockAttendeePopTokens,
          }}
          screenOptions={{ headerLeft: RollCallOpenedHeaderLeft }}
        />
      </FeatureContext.Provider>
    </Provider>,
  );

  act(didFocus);

  return renderedRollCallOpened;
};

beforeEach(() => {
  jest.clearAllMocks();
});

describe('RollCallOpened', () => {
  it('renders correctly when no scan', async () => {
    const { toJSON } = renderRollCallOpened();

    await waitFor(() => {
      expect(toJSON()).toMatchSnapshot();
    });
  });

  it('renders correctly when scanning attendees', async () => {
    const { toJSON } = renderRollCallOpened();

    await waitFor(async () => {
      // scan valid pop tokens
      fakeQrReaderScan(ScannablePopToken.encodePopToken({ pop_token: mockPublicKey2.valueOf() }));
      fakeQrReaderScan(ScannablePopToken.encodePopToken({ pop_token: mockPublicKey3.valueOf() }));
      // scan invalid pop tokens
      fakeQrReaderScan('123');
      fakeQrReaderScan('456');
    });

    expect(toJSON()).toMatchSnapshot();
  });

  it('shows toast when scanning attendees', async () => {
    renderRollCallOpened();

    fakeQrReaderScan(mockPublicKey2.valueOf());
    fakeQrReaderScan(mockPublicKey3.valueOf());

    expect(mockToastShow).toHaveBeenCalledTimes(2);
  });

  it('shows toast when adding an attendee manually', async () => {
    const { getByTestId } = renderRollCallOpened();

    const addAttendeeButton = getByTestId('roll_call_open_add_manually');
    fireEvent.press(addAttendeeButton);

    const textInput = getByTestId('confirm-modal-input');
    fireEvent.changeText(textInput, mockPopToken.publicKey.valueOf());

    const confirmButton = getByTestId('confirm-modal-confirm');
    fireEvent.press(confirmButton);
    await waitFor(() => {
      expect(mockToastShow).toHaveBeenCalledTimes(1);
    });
  });

  it('shows toast when trying to add an incorrect token manually', async () => {
    const { getByTestId } = renderRollCallOpened();

    const addAttendeeButton = getByTestId('roll_call_open_add_manually');
    fireEvent.press(addAttendeeButton);

    const textInput = getByTestId('confirm-modal-input');
    fireEvent.changeText(textInput, 'data');

    const confirmButton = getByTestId('confirm-modal-confirm');
    fireEvent.press(confirmButton);

    await waitFor(() => {
      expect(mockToastShow).toHaveBeenCalledTimes(1);
    });
  });

  it('closes correctly with the current state of scanned attendees', async () => {
    const mockAttendeePopTokens = [mockPopToken.publicKey.valueOf()];

    const button = renderRollCallOpened(mockAttendeePopTokens).getByTestId(
      'roll_call_open_stop_scanning',
    );

    fireEvent.press(button);

    expect(navigate).toHaveBeenCalledWith(expect.anything(), {
      eventId: rollCallId,
      isOrganizer: true,
      attendeePopTokens: mockAttendeePopTokens,
    });
  });

  it('updates the internal state of scanned attendees', async () => {
    const mockAttendeePopTokens = [mockPopToken.publicKey.valueOf()];

    renderRollCallOpened(mockAttendeePopTokens);

    await waitFor(() => {
      fakeQrReaderScan(ScannablePopToken.encodePopToken({ pop_token: mockPublicKey2.valueOf() }));
      fakeQrReaderScan(ScannablePopToken.encodePopToken({ pop_token: mockPublicKey3.valueOf() }));
    });

    expect(setParams).toHaveBeenCalledWith({
      attendeePopTokens: [
        ...mockAttendeePopTokens,
        mockPublicKey2.valueOf(),
        mockPublicKey3.valueOf(),
      ],
    });
  });

  it('shows the correct number of attendees', async () => {
    const mockAttendeePopTokens = [mockPopToken.publicKey.valueOf()];

    const { toJSON } = renderRollCallOpened(mockAttendeePopTokens);

    // counter should be at 0
    expect(toJSON()).toMatchSnapshot();

    await waitFor(() => {
      fakeQrReaderScan(ScannablePopToken.encodePopToken({ pop_token: mockPublicKey2.valueOf() }));
      fakeQrReaderScan(ScannablePopToken.encodePopToken({ pop_token: mockPublicKey3.valueOf() }));
    });

    // counter should be at 2
    expect(toJSON()).toMatchSnapshot();
  });
});
