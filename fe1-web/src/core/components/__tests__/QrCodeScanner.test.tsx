import { render } from '@testing-library/react-native';
import { Camera, PermissionResponse, PermissionStatus } from 'expo-camera';
import React from 'react';
import { Text } from 'react-native';

import QrCodeScanner from '../QrCodeScanner';

jest.mock('expo-camera');

const handleScan = jest.fn();

beforeEach(() => jest.clearAllMocks);

describe('QrCodeScanner', () => {
  it('renders correctly when showing camera', () => {
    const { toJSON } = render(<QrCodeScanner showCamera handleScan={handleScan} />);
    expect(toJSON()).toMatchSnapshot();
  });

  it('renders correctly when not showing camera', () => {
    const { toJSON } = render(<QrCodeScanner showCamera={false} handleScan={handleScan} />);
    expect(toJSON()).toMatchSnapshot();
  });

  it('renders correctly when showing camera with children', () => {
    const { toJSON } = render(
      <QrCodeScanner showCamera handleScan={handleScan}>
        <Text>Text</Text>
      </QrCodeScanner>,
    );
    expect(toJSON()).toMatchSnapshot();
  });

  it('renders correctly when not showing camera with children', () => {
    const { toJSON } = render(
      <QrCodeScanner showCamera={false} handleScan={handleScan}>
        <Text>Text</Text>
      </QrCodeScanner>,
    );
    expect(toJSON()).toMatchSnapshot();
  });

  it('renders correctly when requesting for permissions', () => {
    const useCameraPermissionsMock = jest.spyOn(Camera, 'useCameraPermissions');
    useCameraPermissionsMock.mockImplementation(() => [
      undefined as unknown as PermissionResponse,
      () => Promise.resolve(undefined as unknown as PermissionResponse),
      () => Promise.resolve(undefined as unknown as PermissionResponse),
    ]);
    const { toJSON } = render(<QrCodeScanner showCamera handleScan={handleScan} />);
    expect(toJSON()).toMatchSnapshot();
  });

  it('renders correctly when permissions are denied', () => {
    const response: PermissionResponse = {
      canAskAgain: true,
      expires: 'never',
      granted: false,
      status: PermissionStatus.DENIED,
    };
    const useCameraPermissionsMock = jest.spyOn(Camera, 'useCameraPermissions');
    useCameraPermissionsMock.mockImplementation(() => [
      response,
      () => Promise.resolve(response),
      () => Promise.resolve(response),
    ]);
    const { toJSON } = render(<QrCodeScanner showCamera handleScan={handleScan} />);
    expect(toJSON()).toMatchSnapshot();
  });
});
