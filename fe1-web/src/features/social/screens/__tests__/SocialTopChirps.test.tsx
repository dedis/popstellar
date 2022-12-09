import { configureStore } from '@reduxjs/toolkit';
import { render } from '@testing-library/react-native';
import React from 'react';
import { Provider } from 'react-redux';
import { combineReducers } from 'redux';

import { mockLao, mockLaoId, mockPopToken } from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';
import { laoReducer, setCurrentLao } from 'features/lao/reducer';
import {
  mockChirp0,
  mockChirp1,
  mockChirp2,
  mockReaction1,
  mockReaction2,
  mockReaction4,
} from 'features/social/__tests__/utils';

import { SocialMediaContext } from '../../context';
import { SocialReactContext, SOCIAL_FEATURE_IDENTIFIER } from '../../interface';
import SocialReducer, { addChirp, addReaction } from '../../reducer/SocialReducer';
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

mockStore.dispatch(addChirp(mockLaoId, mockChirp0));
mockStore.dispatch(addChirp(mockLaoId, mockChirp1));
mockStore.dispatch(addChirp(mockLaoId, mockChirp2));

mockStore.dispatch(addReaction(mockLaoId, mockReaction1));
mockStore.dispatch(addReaction(mockLaoId, mockReaction2));
mockStore.dispatch(addReaction(mockLaoId, mockReaction4));

describe('SocialTopChirps', () => {
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
});
