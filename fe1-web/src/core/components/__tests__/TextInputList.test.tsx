import { render, fireEvent } from '@testing-library/react-native';
import React from 'react';

import TextInputList from '../TextInputList';

describe('TextInputList', () => {
  it('renders correctly with a small left trimmed text', () => {
    const { toJSON, getByTestId } = render(<TextInputList onChange={() => {}} testID="x" />);
    const listOption = getByTestId('x_option_0_input');
    fireEvent.changeText(listOption, '    trimmed list   option   ');
    expect(toJSON()).toMatchSnapshot();
  });
});
