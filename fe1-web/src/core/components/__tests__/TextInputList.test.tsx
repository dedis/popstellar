import { fireEvent, render } from '@testing-library/react-native';
import React from 'react';

import TextInputList from '../TextInputList';

describe('TextInputList', () => {
  it('renders correctly', () => {
    const { toJSON, getByTestId } = render(
      <TextInputList values={['text  1  ', '   test 2']} onChange={() => {}} testID="x" />,
    );
    const listOption = getByTestId('x_option_0_input');
    fireEvent.changeText(listOption, '    trimmed list   option   ');
    expect(toJSON()).toMatchSnapshot();
  });
});
