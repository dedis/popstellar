import { configureStore } from '@reduxjs/toolkit';
import { render } from '@testing-library/react-native';
import React from 'react';
import { Provider } from 'react-redux';
import { combineReducers } from 'redux';

import { mockLao, mockLaoId, mockPopToken } from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';
import { PublicKey } from 'core/objects';
import { laoReducer, setCurrentLao } from 'features/lao/reducer';
import { SocialReactContext, SOCIAL_FEATURE_IDENTIFIER } from 'features/social/interface';
import { socialReducer } from 'features/social/reducer';

import Profile from '../Profile';

const publicKey = new PublicKey('PublicKey');

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

const mockStore = configureStore({
  reducer: combineReducers({
    ...laoReducer,
    ...socialReducer,
  }),
});
mockStore.dispatch(setCurrentLao(mockLao));

describe('Profile', () => {
  it('renders correctly', () => {
    const component = render(
      <Provider store={mockStore}>
        <FeatureContext.Provider value={contextValue}>
          <Profile publicKey={publicKey} />
        </FeatureContext.Provider>
      </Provider>,
    ).toJSON();
    expect(component).toMatchSnapshot();
  });
});
