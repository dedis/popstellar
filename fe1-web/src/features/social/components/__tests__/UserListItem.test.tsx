import { configureStore } from '@reduxjs/toolkit';
import { fireEvent, render } from '@testing-library/react-native';
import React from 'react';
import { Provider } from 'react-redux';
import { combineReducers } from 'redux';

import { mockNavigate } from '__mocks__/useNavigationMock';
import { subscribeToChannel } from 'core/network/CommunicationApi';
import { Channel, Hash, PublicKey } from 'core/objects';
import STRINGS from 'resources/strings';

import UserListItem from '../UserListItem';

const publicKey = new PublicKey('PublicKey');
const laoId = new Hash('LaoId');

jest.mock('core/network/CommunicationApi.ts', () => ({
  subscribeToChannel: jest.fn((c: Channel) => Promise.resolve(c)),
}));

jest.mock('core/components/ProfileIcon.tsx', () => () => 'ProfileIcon');

beforeEach(() => {
  mockNavigate.mockClear();
});

const mockStore = configureStore({ reducer: combineReducers({}) });

describe('UserListItem', () => {
  it('calls subscribeToChannel correctly when clicking on follow', () => {
    const expectedChannel = '/root/LaoId/social/PublicKey';
    const button = render(
      <Provider store={mockStore}>
        <UserListItem laoId={laoId} publicKey={publicKey} />
      </Provider>,
    ).getByText(STRINGS.follow_button);

    fireEvent.press(button);

    expect(subscribeToChannel).toHaveBeenCalledTimes(1);
    expect(subscribeToChannel).toHaveBeenCalledWith(
      expect.anything(),
      expect.anything(),
      expectedChannel,
    );
  });

  it('calls navigate correctly when clicking on profile', () => {
    const { getByText } = render(
      <Provider store={mockStore}>
        <UserListItem laoId={laoId} publicKey={publicKey} />
      </Provider>,
    );
    const followButton = getByText(STRINGS.follow_button);
    fireEvent.press(followButton);
    const profileButton = getByText(STRINGS.profile_button);
    fireEvent.press(profileButton);
    expect(mockNavigate).toHaveBeenCalledTimes(1);
    expect(mockNavigate).toHaveBeenCalledWith(STRINGS.social_media_navigation_tab_user_profile, {
      userPkString: publicKey.valueOf(),
    });
  });

  it('renders correctly', () => {
    const component = render(
      <Provider store={mockStore}>
        <UserListItem laoId={laoId} publicKey={publicKey} />
      </Provider>,
    ).toJSON();
    expect(component).toMatchSnapshot();
  });

  it('renders correctly after clicking on follow', () => {
    const { toJSON, getByText } = render(
      <Provider store={mockStore}>
        <UserListItem laoId={laoId} publicKey={publicKey} />
      </Provider>,
    );
    const button = getByText(STRINGS.follow_button);
    fireEvent.press(button);
    expect(toJSON()).toMatchSnapshot();
  });
});
