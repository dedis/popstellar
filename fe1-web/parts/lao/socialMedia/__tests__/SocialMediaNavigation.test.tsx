import React from 'react';
import { render } from '@testing-library/react-native';
import { NavigationContainer } from '@react-navigation/native';
import SocialMediaNavigation from '../SocialMediaNavigation';

describe('SocialMediaNavigation', () => {
  it('renders correctly all the tabs', () => {
    const r = render(
      <NavigationContainer>
        <SocialMediaNavigation />
      </NavigationContainer>
    );

  });
});
