import { render } from '@testing-library/react-native';
import React from 'react';

import { BackRoundButton, LogoutRoundButton } from '../index';

describe('RoundIconButtons', () => {
  it('render correctly', () => {
    const { toJSON } = render(
      <>
        <BackRoundButton onClick={() => {}} />
        <LogoutRoundButton onClick={() => {}} />
      </>,
    );
    expect(toJSON()).toMatchSnapshot();
  });
});
