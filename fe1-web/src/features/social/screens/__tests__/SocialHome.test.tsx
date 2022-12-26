import { configureStore } from '@reduxjs/toolkit';
import { render } from '@testing-library/react-native';
import React from 'react';
import { Provider } from 'react-redux';
import { combineReducers } from 'redux';

import { mockLao, mockLaoId, mockPopToken } from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';
import { laoReducer, setCurrentLao } from 'features/lao/reducer';

import { SocialMediaContext } from '../../context';
import { SocialReactContext, SOCIAL_FEATURE_IDENTIFIER } from '../../interface';
import socialReducer from '../../reducer/SocialReducer';
import SocialHome from '../SocialHome';

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
    ...socialReducer,
  }),
});
mockStore.dispatch(setCurrentLao(mockLao));

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
});
