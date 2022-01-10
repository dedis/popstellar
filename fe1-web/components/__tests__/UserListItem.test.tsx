import React from 'react';
import { fireEvent, render } from '@testing-library/react-native';
import STRINGS from 'res/strings';
import { Channel, Hash, PublicKey } from 'model/objects';
import { subscribeToChannel } from 'network/CommunicationApi';
import UserListItem from '../UserListItem';

const publicKey = new PublicKey('PublicKey');
const laoId = new Hash('LaoId');

jest.mock('network/CommunicationApi.ts', () => ({
  subscribeToChannel: jest.fn((c: Channel) => Promise.resolve(c)),
}));

jest.mock('components/ProfileIcon.tsx', () => () => 'ProfileIcon');

describe('UserListItem', () => {
  it('calls subscribeToChannel correctly when clicking on follow', () => {
    const expectedChannel = '/root/LaoId/social/PublicKey';
    const button = render(
      <UserListItem laoId={laoId} publicKey={publicKey} />,
    ).getByText(STRINGS.follow_button);
    fireEvent.press(button);
    expect(subscribeToChannel).toHaveBeenCalledTimes(1);
    expect(subscribeToChannel).toHaveBeenCalledWith(expectedChannel);
  });

  it('renders correctly', () => {
    const component = render(
      <UserListItem laoId={laoId} publicKey={publicKey} />,
    ).toJSON();
    expect(component).toMatchSnapshot();
  });

  it('renders correctly after clicking on follow', () => {
    const { toJSON, getByText } = render(
      <UserListItem laoId={laoId} publicKey={publicKey} />,
    );
    const button = getByText(STRINGS.follow_button);
    fireEvent.press(button);
    expect(toJSON()).toMatchSnapshot();
  });
});
