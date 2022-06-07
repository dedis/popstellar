import { useNavigation } from '@react-navigation/core';
import { fireEvent, render, waitFor } from '@testing-library/react-native';
import React from 'react';
// @ts-ignore
import { fireScan as fakeQrReaderScan } from 'react-qr-reader';
import { Provider } from 'react-redux';
import { act } from 'react-test-renderer';
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

import { requestCloseRollCall as mockRequestCloseRollCall } from '../../network/RollCallMessageApi';
import RollCallOpened from '../RollCallOpened';

const mockPublicKey2 = new PublicKey('mockPublicKey2_fFcHDaVHcCcY8IBfHE7auXJ7h4ms=');
const mockPublicKey3 = new PublicKey('mockPublicKey3_fFcHDaVHcCcY8IBfHE7auXJ7h4ms=');

jest.mock('@react-navigation/core', () => {
  const actualNavigation = jest.requireActual('@react-navigation/core');

  const navigate = jest.fn();
  const addListener = jest.fn();

  return {
    ...actualNavigation,
    useNavigation: () => ({
      navigate,
      addListener,
    }),
  };
});
jest.mock('react-qr-reader');
jest.mock('features/rollCall/network/RollCallMessageApi');

// just a mock hook
// eslint-disable-next-line react-hooks/rules-of-hooks
const { navigate, addListener } = useNavigation();
const mockToastShow = jest.fn();
const mockToastRet = {
  show: mockToastShow,
};
jest.mock('react-native-toast-notifications', () => ({
  useToast: () => mockToastRet,
}));

(mockRequestCloseRollCall as jest.Mock).mockImplementation(() => Promise.resolve());

const mockRollCall = RollCall.fromState({ ...mockRollCallWithAliasState, attendees: [] });
const rollCallId = mockRollCallWithAlias.id.valueOf();

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

const didFocus = () =>
  // call focus event listener
  (addListener as jest.Mock).mock.calls
    .filter(([eventName]) => eventName === 'focus')
    .forEach((args) => args[1]());

const renderRollCallOpened = () => {
  const renderedRollCallOpened = render(
    <Provider store={mockStore}>
      <FeatureContext.Provider value={contextValue}>
        <MockNavigator component={RollCallOpened} params={{ rollCallId }} />
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
    const { getByTestId } = renderRollCallOpened();

    const addAttendeeButton = getByTestId('roll-call-open-add-manually');
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

    const addAttendeeButton = getByTestId('roll-call-open-add-manually');
    fireEvent.press(addAttendeeButton);

    const textInput = getByTestId('confirm-modal-input');
    fireEvent.changeText(textInput, 'data');

    const confirmButton = getByTestId('confirm-modal-confirm');
    fireEvent.press(confirmButton);

    await waitFor(() => {
      expect(mockToastShow).toHaveBeenCalledTimes(1);
    });
  });

  it('closes correctly with no attendee', async () => {
    const button = renderRollCallOpened().getByTestId('roll-call-open-stop-scanning');

    await waitFor(() => {
      expect(mockGenerateToken).toHaveBeenCalled();
    });

    fireEvent.press(button);

    expect(navigate).toHaveBeenCalledWith(expect.anything(), {
      eventId: rollCallId,
      isOrganizer: true,
      attendeePopTokens: [mockPopToken.publicKey.valueOf()],
    });
  });

  it('closes correctly with two attendees', async () => {
    const button = renderRollCallOpened().getByTestId('roll-call-open-stop-scanning');

    await waitFor(() => {
      fakeQrReaderScan(mockPublicKey2.valueOf());
      fakeQrReaderScan(mockPublicKey3.valueOf());
      expect(mockGenerateToken).toHaveBeenCalled();
    });
    fireEvent.press(button);

    expect(navigate).toHaveBeenCalledWith(expect.anything(), {
      eventId: rollCallId,
      isOrganizer: true,
      attendeePopTokens: [
        mockPublicKey2.valueOf(),
        mockPublicKey3.valueOf(),
        mockPopToken.publicKey.valueOf(),
      ],
    });
  });
});
