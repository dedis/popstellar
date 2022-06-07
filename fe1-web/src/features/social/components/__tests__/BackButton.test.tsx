import { fireEvent, render } from '@testing-library/react-native';
import React from 'react';

import { mockNavigate } from '__mocks__/useNavigationMock';
import STRINGS from 'resources/strings';

import BackButton from '../BackButton';

beforeEach(() => {
  mockNavigate.mockClear();
});

describe('BackButton', () => {
  it('renders correctly', () => {
    const component = render(
      <BackButton navigationTabName={STRINGS.social_media_navigation_tab_search} />,
    ).toJSON();
    expect(component).toMatchSnapshot();
  });

  it('navigates correctly', () => {
    const button = render(
      <BackButton navigationTabName={STRINGS.social_media_navigation_tab_search} />,
    ).getByTestId('backButton');
    fireEvent.press(button);
    expect(mockNavigate).toHaveBeenCalledWith(STRINGS.social_media_navigation_tab_search);
    expect(mockNavigate).toHaveBeenCalledTimes(1);
  });
});
