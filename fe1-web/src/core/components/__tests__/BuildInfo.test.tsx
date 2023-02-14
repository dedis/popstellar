import { render } from '@testing-library/react-native';
import React from 'react';

import BuildInfo from '../BuildInfo';

describe('BuildInfo', () => {
  it('renders correctly', () => {
    const { toJSON } = render(<BuildInfo />);
    expect(toJSON()).toMatchSnapshot();
  });
});
