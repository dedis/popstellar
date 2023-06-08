import { configureStore } from '@reduxjs/toolkit';
import { fireEvent, render, waitFor } from '@testing-library/react-native';
import React from 'react';
import { Provider } from 'react-redux';
import { combineReducers } from 'redux';

import { mockLao, mockLaoId, mockPopToken } from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';
import { laoReducer, setCurrentLao } from 'features/lao/reducer';
import STRINGS from 'resources/strings';

import { SocialMediaContext } from '../../context';
import { SOCIAL_FEATURE_IDENTIFIER, SocialReactContext } from '../../interface';
import { requestAddChirp } from '../../network/SocialMessageApi';
import SocialReducer from '../../reducer/SocialReducer';
import NewChirp from '../NewChirp';

jest.mock('features/social/network/SocialMessageApi', () => {
  const actual = jest.requireActual('features/social/network/SocialMessageApi');
  return {
    ...actual,
    requestAddChirp: jest.fn(() => Promise.resolve()),
  };
});

const contextValue = {
  [SOCIAL_FEATURE_IDENTIFIER]: {
    useCurrentLao: () => mockLao,
    getCurrentLao: () => mockLao,
    useConnectedToLao: () => true,
    useCurrentLaoId: () => mockLaoId,
    getCurrentLaoId: () => mockLaoId,
    useRollCallById: () => undefined,
    useRollCallAttendeesById: () => [],
    generateToken: () => Promise.resolve(mockPopToken),
  } as SocialReactContext,
};

const socialContextValue = {
  currentUserPopTokenPublicKey: mockPopToken.publicKey,
};

beforeEach(() => {
  jest.clearAllMocks();
});

const mockStore = configureStore({
  reducer: combineReducers({
    ...laoReducer,
    ...SocialReducer,
  }),
});
mockStore.dispatch(setCurrentLao(mockLao));

describe('NewChirp', () => {
  it('renders correctly', () => {
    const { toJSON } = render(
      <Provider store={mockStore}>
        <FeatureContext.Provider value={contextValue}>
          <SocialMediaContext.Provider value={socialContextValue}>
            <NewChirp />
          </SocialMediaContext.Provider>
        </FeatureContext.Provider>
      </Provider>,
    );
    expect(toJSON()).toMatchSnapshot();
  });

  it('is possible to publish chirps', () => {
    const { getByTestId } = render(
      <Provider store={mockStore}>
        <FeatureContext.Provider value={contextValue}>
          <SocialMediaContext.Provider value={socialContextValue}>
            <NewChirp />
          </SocialMediaContext.Provider>
        </FeatureContext.Provider>
      </Provider>,
    );

    const mockText = 'some chirp text';

    fireEvent.changeText(getByTestId('new_chirp_input'), mockText);
    fireEvent.press(getByTestId('new_chirp_publish'));

    expect(requestAddChirp).toHaveBeenCalledWith(mockPopToken.publicKey, mockText, mockLaoId);
    expect(requestAddChirp).toHaveBeenCalledTimes(1);
  });

  it('shows the modal on trimmed chirp and closes it', () => {
    const { toJSON, getByTestId, queryByText } = render(
      <Provider store={mockStore}>
        <FeatureContext.Provider value={contextValue}>
          <SocialMediaContext.Provider value={socialContextValue}>
            <NewChirp />
          </SocialMediaContext.Provider>
        </FeatureContext.Provider>
      </Provider>,
    );
    const mockText = '\nThis    text\n \twill be trimmed \n ';

    // Add an invalid chirp
    fireEvent.changeText(getByTestId('new_chirp_input'), mockText);
    fireEvent.press(getByTestId('new_chirp_publish'));

    expect(getByTestId('confirm-modal-confirm')).toBeDefined();
    expect(toJSON()).toMatchSnapshot();

    // Accept the modal message
    fireEvent.press(getByTestId('confirm-modal-confirm'));

    waitFor(() => expect(queryByText('300')).not.toBeNull());
    expect(toJSON()).toMatchSnapshot();
  });
  it('shows the error message on empty trimmed message', () => {
    const { toJSON, queryByText, getByTestId } = render(
      <Provider store={mockStore}>
        <FeatureContext.Provider value={contextValue}>
          <SocialMediaContext.Provider value={socialContextValue}>
            <NewChirp />
          </SocialMediaContext.Provider>
        </FeatureContext.Provider>
      </Provider>,
    );

    const mockText = '   \t\n \r\n  ';

    fireEvent.changeText(getByTestId('new_chirp_input'), mockText);

    expect(queryByText(STRINGS.social_media_empty_chirp)).not.toBeNull();
    expect(toJSON()).toMatchSnapshot();
  });
});
