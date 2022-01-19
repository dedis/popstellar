import React from 'react';
import { fireEvent, render } from '@testing-library/react-native';
import STRINGS from 'res/strings';
import { Channel, Hash, PublicKey } from 'model/objects';
import { subscribeToChannel } from 'network/CommunicationApi';
import keyPair from 'test_data/keypair.json';
import UserListItem from '../UserListItem';

const publicKey = new PublicKey('PublicKey');
const mockPublicKey = new PublicKey(keyPair.publicKey);
const laoId = new Hash('LaoId');
const mockNavigate = jest.fn();

jest.mock('@react-navigation/native', () => {
  const actualNavigation = jest.requireActual('@react-navigation/native');
  return {
    ...actualNavigation,
    useNavigation: () => ({
      navigate: mockNavigate,
    }),
  };
});

jest.mock('network/CommunicationApi.ts', () => ({
  subscribeToChannel: jest.fn((c: Channel) => Promise.resolve(c)),
}));

jest.mock('components/ProfileIcon.tsx', () => () => 'ProfileIcon');

describe('UserListItem', () => {
  it('calls subscribeToChannel correctly when clicking on follow', () => {
    const expectedChannel = '/root/LaoId/social/PublicKey';
    const button = render(
      <UserListItem laoId={laoId} publicKey={publicKey} currentUserPublicKey={mockPublicKey} />,
    ).getByText(STRINGS.follow_button);
    fireEvent.press(button);
    expect(subscribeToChannel).toHaveBeenCalledTimes(1);
    expect(subscribeToChannel).toHaveBeenCalledWith(expectedChannel);
  });

  it('calls navigate correctly when clicking on profile', () => {
    const { getByText } = render(
      <UserListItem laoId={laoId} publicKey={publicKey} currentUserPublicKey={mockPublicKey} />,
    );
    const followButton = getByText(STRINGS.follow_button);
    fireEvent.press(followButton);
    const profileButton = getByText(STRINGS.profile_button);
    fireEvent.press(profileButton);
    expect(mockNavigate).toHaveBeenCalledTimes(1);
    expect(mockNavigate).toHaveBeenCalledWith(
      STRINGS.social_media_navigation_tab_user_profile,
      { currentUserPublicKey: mockPublicKey, userPublicKey: publicKey },
    );
  });

  it('renders correctly', () => {
    const component = render(
      <UserListItem laoId={laoId} publicKey={publicKey} currentUserPublicKey={mockPublicKey} />,
    ).toJSON();
    expect(component).toMatchSnapshot();
  });

  it('renders correctly after clicking on follow', () => {
    const { toJSON, getByText } = render(
      <UserListItem laoId={laoId} publicKey={publicKey} currentUserPublicKey={mockPublicKey} />,
    );
    const button = getByText(STRINGS.follow_button);
    fireEvent.press(button);
    expect(toJSON()).toMatchSnapshot();
  });
});
