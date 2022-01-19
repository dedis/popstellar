import React from 'react';
import { render } from '@testing-library/react-native';
import { PublicKey } from 'model/objects';
import SocialProfile from '../SocialProfile';

const mockEmptyPublicKey = new PublicKey('');

jest.mock('react-redux', () => ({
  ...jest.requireActual('react-redux'),
  useSelector: jest.fn().mockImplementation(() => []),
}));

describe('SocialProfile', () => {
  it('renders correctly with an empty user public key', () => {
    const component = render(<SocialProfile currentUserPublicKey={mockEmptyPublicKey} />).toJSON();
    expect(component).toMatchSnapshot();
  });
});
