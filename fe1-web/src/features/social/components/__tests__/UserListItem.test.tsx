import { configureStore } from '@reduxjs/toolkit';
import { fireEvent, render } from '@testing-library/react-native';
import React from 'react';
import { Provider } from 'react-redux';
import { combineReducers } from 'redux';

import { mockNavigate } from '__mocks__/useNavigationMock';
import MockNavigator from '__tests__/components/MockNavigator';
import { mockLao, mockLaoId, mockPopToken } from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';
import { Channel, PublicKey } from 'core/objects';
import { SocialReactContext, SOCIAL_FEATURE_IDENTIFIER } from 'features/social/interface';
import STRINGS from 'resources/strings';

import UserListItem from '../UserListItem';

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

jest.mock('core/network/CommunicationApi.ts', () => ({
  subscribeToChannel: jest.fn((c: Channel) => Promise.resolve(c)),
}));

jest.mock('core/components/ProfileIcon.tsx', () => () => 'ProfileIcon');

beforeEach(() => {
  mockNavigate.mockClear();
});

const mockStore = configureStore({ reducer: combineReducers({}) });

describe('UserListItem', () => {
  it('calls navigate correctly when clicking on profile', () => {
    const { getByTestId } = render(
      <Provider store={mockStore}>
        <FeatureContext.Provider value={contextValue}>
          <MockNavigator
            component={() => (
              <UserListItem publicKey={publicKey} isFirstItem={false} isLastItem={false} />
            )}
          />
        </FeatureContext.Provider>
      </Provider>,
    );
    const profileButton = getByTestId(`user_list_item_${publicKey.toString()}`);
    fireEvent.press(profileButton);
    expect(mockNavigate).toHaveBeenCalledTimes(1);
    expect(mockNavigate).toHaveBeenCalledWith(STRINGS.social_media_navigation_user_profile, {
      userPkString: publicKey.valueOf(),
    });
  });

  it('renders correctly', () => {
    const component = render(
      <Provider store={mockStore}>
        <FeatureContext.Provider value={contextValue}>
          <MockNavigator
            component={() => (
              <UserListItem publicKey={publicKey} isFirstItem={false} isLastItem={false} />
            )}
          />
        </FeatureContext.Provider>
      </Provider>,
    ).toJSON();
    expect(component).toMatchSnapshot();
  });
});
