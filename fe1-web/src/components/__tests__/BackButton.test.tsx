import React from 'react';
import { fireEvent, render } from '@testing-library/react-native';
import BackButton from '../BackButton';

const mockHomeTab = 'Home';
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

describe('BackButton', () => {
  it('renders correctly', () => {
    const component = render(<BackButton navigationTabName={mockHomeTab} />).toJSON();
    expect(component).toMatchSnapshot();
  });

  it('navigates correctly', () => {
    const button = render(<BackButton navigationTabName={mockHomeTab} />).getByTestId('backButton');
    fireEvent.press(button);
    expect(mockNavigate).toHaveBeenCalledWith(mockHomeTab);
    expect(mockNavigate).toHaveBeenCalledTimes(1);
  });
});
