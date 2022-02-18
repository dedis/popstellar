import React from 'react';
import { fireEvent, render } from '@testing-library/react-native';
import { mockNavigate } from '__mocks__/useNavigationMock';
import BackButton from 'core/components/BackButton';

const mockHomeTab = 'Home';

beforeEach(() => {
  mockNavigate.mockClear();
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
