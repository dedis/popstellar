import { fireEvent, render, waitFor } from '@testing-library/react-native';
import React from 'react';

import { fireScan } from '__mocks__/expo-camera';
import { mockLaoId } from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';
import STRINGS from 'resources/strings';

import { POPCHA_FEATURE_IDENTIFIER, PopchaReactContext } from '../../interface';
import { sendPopchaAuthRequest } from '../../network/PopchaMessageApi';
import PopchaScanner from '../PopchaScanner';

const contextValue = {
  [POPCHA_FEATURE_IDENTIFIER]: {
    useCurrentLaoId: () => mockLaoId,
  } as PopchaReactContext,
};

const mockToastShow = jest.fn();
const mockToastRet = {
  show: mockToastShow,
};
jest.mock('react-native-toast-notifications', () => ({
  useToast: () => mockToastRet,
}));

jest.mock('../../network/PopchaMessageApi', () => ({
  sendPopchaAuthRequest: jest.fn(() => Promise.resolve()),
}));

const mockClientId = 'mockClientId';
const mockRedirectUri = 'mockRedirectUri';
const mockLoginHint = mockLaoId.toString();
const mockNonce = 'mockNonce';
const mockResponseType = 'id_token';
const mockScope = 'openid profile';

const mockUrl = new URL('https://valid2.server.example:8000');
mockUrl.searchParams.append('client_id', mockClientId);
mockUrl.searchParams.append('redirect_uri', mockRedirectUri);
mockUrl.searchParams.append('login_hint', mockLoginHint);
mockUrl.searchParams.append('nonce', mockNonce);
mockUrl.searchParams.append('response_type', mockResponseType);
mockUrl.searchParams.append('scope', mockScope);

beforeEach(() => {
  jest.clearAllMocks();
});

/**
 * Test that the scanner shows an error message when the url is invalid
 * @param url an invalid url to test
 */
const testInvalidUrl = async (url: string) => {
  const { getByTestId } = render(
    <FeatureContext.Provider value={contextValue}>
      <PopchaScanner />
    </FeatureContext.Provider>,
  );
  const scannerButton = getByTestId('popcha_scanner_button');
  fireEvent.press(scannerButton);
  fireScan(url);
  await waitFor(() => expect(mockToastShow).toHaveBeenCalledTimes(1));
};

describe('Popcha scanner', () => {
  describe('scanner renders correctly', () => {
    it('renders correctly a closed scanner', () => {
      const { toJSON } = render(
        <FeatureContext.Provider value={contextValue}>
          <PopchaScanner />
        </FeatureContext.Provider>,
      );
      expect(toJSON()).toMatchSnapshot();
    });

    it('renders correctly a opened scanner', () => {
      const { getByTestId, toJSON } = render(
        <FeatureContext.Provider value={contextValue}>
          <PopchaScanner />
        </FeatureContext.Provider>,
      );
      const button = getByTestId('popcha_scanner_button');
      fireEvent.press(button);
      expect(toJSON()).toMatchSnapshot();
    });
  });
  describe('scanner verifies correctly', () => {
    it('shows error message with invalid url format', async () => {
      await testInvalidUrl('invalid url');
    });

    it('shows error message with url without client_id', async () => {
      const url = new URL(mockUrl.toString());
      url.searchParams.delete('client_id');
      await testInvalidUrl(url.toString());
    });

    it('shows error message with url without redirect_uri', async () => {
      const url = new URL(mockUrl.toString());
      url.searchParams.delete('redirect_uri');
      await testInvalidUrl(url.toString());
    });

    it('shows error message with url without login_hint', async () => {
      const url = new URL(mockUrl.toString());
      url.searchParams.delete('login_hint');
      await testInvalidUrl(url.toString());
    });

    it('shows error message with url without nonce', async () => {
      const url = new URL(mockUrl.toString());
      url.searchParams.delete('nonce');
      await testInvalidUrl(url.toString());
    });

    it('shows error message with url without response_type', async () => {
      const url = new URL(mockUrl.toString());
      url.searchParams.delete('response_type');
      await testInvalidUrl(url.toString());
    });

    it('shows error message with url without scope', async () => {
      const url = new URL(mockUrl.toString());
      url.searchParams.delete('scope');
      await testInvalidUrl(url.toString());
    });

    it('shows error message with url with invalid scope', async () => {
      const url = new URL(mockUrl.toString());
      url.searchParams.set('scope', 'invalid');
      await testInvalidUrl(url.toString());
    });

    it('shows error message with url with invalid response_type', async () => {
      const url = new URL(mockUrl.toString());
      url.searchParams.set('response_type', 'scope invalid response type');
      await testInvalidUrl(url.toString());
    });

    it('shows error message with url with invalid response_mode', async () => {
      const url = new URL(mockUrl.toString());
      url.searchParams.set('response_mode', 'invalid response mode');
      await testInvalidUrl(url.toString());
    });

    it('shows error message when login_hint does not match current laoId', async () => {
      const url = new URL(mockUrl.toString());
      url.searchParams.set('login_hint', 'invalid login hint');
      await testInvalidUrl(url.toString());
    });
  });

  describe('valid url sends correct response', () => {
    it('sends correct response with valid url', async () => {
      const { getByTestId } = render(
        <FeatureContext.Provider value={contextValue}>
          <PopchaScanner />
        </FeatureContext.Provider>,
      );

      const scannerButton = getByTestId('popcha_scanner_button');
      fireEvent.press(scannerButton);
      fireScan(mockUrl.toString());
      await waitFor(() => expect(mockToastShow).toHaveBeenCalledTimes(0));
    });

    it('does not show error message with valid response mode', async () => {
      const url = new URL(mockUrl.toString());
      url.searchParams.set('response_mode', 'query');
      const { getByTestId } = render(
        <FeatureContext.Provider value={contextValue}>
          <PopchaScanner />
        </FeatureContext.Provider>,
      );

      const scannerButton = getByTestId('popcha_scanner_button');
      fireEvent.press(scannerButton);
      fireScan(url.toString());
      await waitFor(() => expect(mockToastShow).toHaveBeenCalledTimes(0));
    });
  });

  describe('correct behavior when sending request', () => {
    it('closes scanner when it is a successful request', async () => {
      const url = new URL(mockUrl.toString());
      (sendPopchaAuthRequest as jest.Mock).mockReturnValue(Promise.resolve());
      const { getByTestId, getByText, toJSON } = render(
        <FeatureContext.Provider value={contextValue}>
          <PopchaScanner />
        </FeatureContext.Provider>,
      );
      const scannerButton = getByTestId('popcha_scanner_button');
      fireEvent.press(scannerButton);
      fireScan(url.toString());
      await waitFor(() => {
        // wait for scanner to close
        expect(getByText(STRINGS.popcha_open_scanner)).toBeTruthy();
        expect(mockToastShow).toHaveBeenCalledTimes(0);
        expect(toJSON()).toMatchSnapshot();
      });
    });

    it('shows error message with failed request', async () => {
      const url = new URL(mockUrl.toString());
      (sendPopchaAuthRequest as jest.Mock).mockReturnValue(Promise.reject(new Error('error')));
      const { getByTestId, toJSON } = render(
        <FeatureContext.Provider value={contextValue}>
          <PopchaScanner />
        </FeatureContext.Provider>,
      );
      const scannerButton = getByTestId('popcha_scanner_button');
      fireEvent.press(scannerButton);
      fireScan(url.toString());
      await waitFor(() => {
        expect(toJSON()).toMatchSnapshot();
        expect(mockToastShow).toHaveBeenCalledTimes(1);
      });
    });
  });
});
