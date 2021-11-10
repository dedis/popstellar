import React from 'react';
import { render } from '@testing-library/react-native';
import SocialMediaNavigation from '../SocialMediaNavigation';

jest.mock('react-native/Libraries/Animated/NativeAnimatedHelper');

describe('SocialMediaNavigation', () => {
  it('renders correctly all the tabs', () => {
    const { toJson } = render(
      <SocialMediaNavigation />,
    );
    expect(toJson()).toMatchSnapshot();
  });
});
