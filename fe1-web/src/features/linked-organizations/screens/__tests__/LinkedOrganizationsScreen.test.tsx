import { configureStore } from '@reduxjs/toolkit';
import { fireEvent, render, waitFor } from '@testing-library/react-native';
import React from 'react';
import { Provider } from 'react-redux';
import { combineReducers } from 'redux';

import MockNavigator from '__tests__/components/MockNavigator';
import { mockLaoId } from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';
import {
  LINKED_ORGANIZATIONS_FEATURE_IDENTIFIER,
  LinkedOrganizationsReactContext,
} from 'features/linked-organizations/interface';

import LinkedOrganizationsScreen from '../LinkedOrganizationsScreen';

const mockLinkedOrganizationsContextValue = (isOrganizer: boolean) => ({
  [LINKED_ORGANIZATIONS_FEATURE_IDENTIFIER]: {
    useCurrentLaoId: () => mockLaoId,
    useConnectedToLao: () => true,
    useIsLaoOrganizer: () => isOrganizer,
  } as LinkedOrganizationsReactContext,
});

// Set up mock store
const mockStore = configureStore({
  reducer: combineReducers({}),
});

jest.mock('react-native-toast-notifications', () => ({
  useToast: jest.fn(() => ({
    show: jest.fn(),
  })),
}));

const renderLinkedOrganizationsScreen = () =>
  render(
    <Provider store={mockStore}>
      <FeatureContext.Provider value={mockLinkedOrganizationsContextValue(true)}>
        <MockNavigator component={LinkedOrganizationsScreen} />
      </FeatureContext.Provider>
    </Provider>,
  );

describe('LinkedOrganizationsScreenNoOrganizer', () => {
  it('renders correctly no organizer', () => {
    const renderLinkedOrganizationsScreen2 = () =>
      render(
        <Provider store={mockStore}>
          <FeatureContext.Provider value={mockLinkedOrganizationsContextValue(false)}>
            <MockNavigator component={LinkedOrganizationsScreen} />
          </FeatureContext.Provider>
        </Provider>,
      );
    const { toJSON } = renderLinkedOrganizationsScreen2();
    expect(toJSON()).toMatchSnapshot();
  });
});

describe('LinkedOrganizationsScreen', () => {
  it('renders correctly', () => {
    const { toJSON } = renderLinkedOrganizationsScreen();
    expect(toJSON()).toMatchSnapshot();
  });

  it('opens modal on FAB press', () => {
    const { getByTestId, toJSON } = renderLinkedOrganizationsScreen();
    fireEvent.press(getByTestId('fab-button'));
    expect(getByTestId('modal-add-organization').props.visible).toBe(true);
    expect(toJSON()).toMatchSnapshot();
  });

  it('opens modal on press join org', () => {
    const { getByTestId, toJSON } = renderLinkedOrganizationsScreen();
    fireEvent.press(getByTestId('fab-button'));
    fireEvent.press(getByTestId('show-qr-code'));
    expect(getByTestId('modal-show-qr-code').props.visible).toBe(true);
    expect(toJSON()).toMatchSnapshot();
  });

  it('opens modal on press link org', () => {
    const { getByTestId, toJSON } = renderLinkedOrganizationsScreen();
    fireEvent.press(getByTestId('fab-button'));
    fireEvent.press(getByTestId('show-scanner'));
    expect(getByTestId('modal-show-scanner').props.visible).toBe(true);
    expect(toJSON()).toMatchSnapshot();
  });

  it('opens modal on press next', () => {
    const { getByTestId, toJSON } = renderLinkedOrganizationsScreen();
    fireEvent.press(getByTestId('fab-button'));
    fireEvent.press(getByTestId('show-qr-code'));
    expect(getByTestId('modal-show-qr-code').props.visible).toBe(true);
    fireEvent.press(getByTestId('button-next-finish'));
    expect(getByTestId('modal-show-scanner').props.visible).toBe(true);
    expect(toJSON()).toMatchSnapshot();
  });

  it('input of correct organization details manually', async () => {
    const { getByTestId, toJSON } = renderLinkedOrganizationsScreen();
    fireEvent.press(getByTestId('fab-button'));
    fireEvent.press(getByTestId('show-scanner'));
    await waitFor(() => {
      expect(getByTestId('open_add_manually')).toBeTruthy();
    });
    fireEvent(getByTestId('open_add_manually'), 'click');
    const laoIdInput = getByTestId('modal-input-laoid');
    fireEvent.changeText(laoIdInput, 'fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo=');
    const publicKeyInput = getByTestId('modal-input-publickey');
    fireEvent.changeText(publicKeyInput, 'J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=');
    const serverAddressInput = getByTestId('modal-input-serveraddress');
    fireEvent.changeText(serverAddressInput, 'wss://epfl.ch:9000/server');
    const challengeValueInput = getByTestId('modal-input-challengeval');
    fireEvent.changeText(
      challengeValueInput,
      '82520f235f413b26571529f69d53d751335873efca97e15cd7c47d063ead830d',
    );
    fireEvent.press(getByTestId('add-manually'));
    expect(toJSON()).toMatchSnapshot();
  });

  it('input of wrong organization details manually', async () => {
    const { getByTestId, toJSON } = renderLinkedOrganizationsScreen();
    fireEvent.press(getByTestId('fab-button'));
    fireEvent.press(getByTestId('show-scanner'));
    await waitFor(() => {
      expect(getByTestId('open_add_manually')).toBeTruthy();
    });
    fireEvent(getByTestId('open_add_manually'), 'click');
    const laoIdInput = getByTestId('modal-input-laoid');
    fireEvent.changeText(laoIdInput, 'acb123');
    const publicKeyInput = getByTestId('modal-input-publickey');
    fireEvent.changeText(publicKeyInput, 'abc123');
    const serverAddressInput = getByTestId('modal-input-serveraddress');
    fireEvent.changeText(serverAddressInput, 'abc123');
    const challengeValueInput = getByTestId('modal-input-challengeval');
    fireEvent.changeText(challengeValueInput, 'abc123');
    fireEvent.press(getByTestId('add-manually'));
    expect(toJSON()).toMatchSnapshot();
  });
});
