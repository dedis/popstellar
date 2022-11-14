import { configureStore } from '@reduxjs/toolkit';
import { fireEvent, render, waitFor } from '@testing-library/react-native';
import React from 'react';
import { Provider } from 'react-redux';
import { combineReducers } from 'redux';

import { mockLao, mockLaoIdHash, mockLaoState, mockPopToken } from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';
import { laoReducer, setCurrentLao } from 'features/lao/reducer';

import { SocialMediaContext } from '../../context';
import { SocialReactContext, SOCIAL_FEATURE_IDENTIFIER } from '../../interface';
import { requestAddChirp } from '../../network/SocialMessageApi';
import SocialReducer from '../../reducer/SocialReducer';
import SocialHome from '../SocialHome';

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
    useCurrentLaoId: () => mockLaoIdHash,
    getCurrentLaoId: () => mockLaoIdHash,
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
mockStore.dispatch(setCurrentLao({ lao: mockLaoState }));

describe('SocialHome', () => {
  it('renders correctly', () => {
    const { toJSON } = render(
      <Provider store={mockStore}>
        <FeatureContext.Provider value={contextValue}>
          <SocialMediaContext.Provider value={socialContextValue}>
            <SocialHome />
          </SocialMediaContext.Provider>
        </FeatureContext.Provider>
      </Provider>,
    );
    expect(toJSON()).toMatchSnapshot();
  });

  it('is possible to publish chirps', async () => {
    const { getByTestId } = render(
      <Provider store={mockStore}>
        <FeatureContext.Provider value={contextValue}>
          <SocialMediaContext.Provider value={socialContextValue}>
            <SocialHome />
          </SocialMediaContext.Provider>
        </FeatureContext.Provider>
      </Provider>,
    );

    const mockText = 'some chirp text';

    fireEvent.changeText(getByTestId('new_chirp_input'), mockText);
    fireEvent.press(getByTestId('new_chirp_publish'));

    await waitFor(() => {
      expect(requestAddChirp).toHaveBeenCalledWith(mockPopToken.publicKey, mockText, mockLaoIdHash);
      expect(requestAddChirp).toHaveBeenCalledTimes(1);
    });
  });
});
