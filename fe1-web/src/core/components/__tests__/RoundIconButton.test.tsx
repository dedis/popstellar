import { render } from '@testing-library/react-native';
import React from 'react';

import { BackRoundButton, LogoutRoundButton } from '../index';

describe('RoundIconButton', () => {
  it('back version renders correctly', () => {
    const { toJSON } = render(<BackRoundButton onClick={() => {}} />);
    expect(toJSON()).toMatchSnapshot();
  });
  it('logout version renders correctly', () => {
    const { toJSON } = render(<LogoutRoundButton onClick={() => {}} />);
    expect(toJSON()).toMatchSnapshot();
  });
});
