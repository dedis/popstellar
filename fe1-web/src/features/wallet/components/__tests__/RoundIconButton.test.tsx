import { render } from '@testing-library/react-native';
import React from 'react';

import { RoundIconButton } from '../index';

describe('RoundIconButton', () => {
  it('renders correctly', () => {
    const { toJSON } = render(<RoundIconButton name="close" onClick={() => {}} />);
    expect(toJSON()).toMatchSnapshot();
  });
});
