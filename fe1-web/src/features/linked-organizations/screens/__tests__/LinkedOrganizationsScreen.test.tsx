import { configureStore } from '@reduxjs/toolkit';
import { fireEvent, render } from '@testing-library/react-native';
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

const renderLinkedOrganizationsScreen = () =>
  render(
    <Provider store={mockStore}>
      <FeatureContext.Provider value={mockLinkedOrganizationsContextValue(true)}>
        <MockNavigator component={LinkedOrganizationsScreen} />
      </FeatureContext.Provider>
    </Provider>,
  );

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
});
