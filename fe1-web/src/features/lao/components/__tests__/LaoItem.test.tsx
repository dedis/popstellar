import { describe } from '@jest/globals';
import { render } from '@testing-library/react-native';
import React from 'react';

import MockNavigator from '__tests__/components/MockNavigator';
import { mockLao } from '__tests__/utils';

import LaoItem from '../LaoItem';

describe('LaoItem', () => {
  it('renders correctly', () => {
    const component = render(
      <MockNavigator component={() => <LaoItem lao={mockLao} />} />,
    ).toJSON();
    expect(component).toMatchSnapshot();
  });
});
