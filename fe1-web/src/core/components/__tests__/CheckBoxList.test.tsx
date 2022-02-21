import React from 'react';
import { fireEvent, render } from '@testing-library/react-native';

import CheckboxList from '../CheckboxList';

let onChange: Function;
const question = {
  title: 'What is the answer of life?',
  data: ['Yes', '42', 'Cats'],
};

beforeEach(() => {
  onChange = jest.fn();
});

describe('CheckBoxList', () => {
  it('renders correctly', () => {
    const component = render(
      <CheckboxList title={question.title} onChange={onChange} values={question.data} />,
    ).toJSON();
    expect(component).toMatchSnapshot();
  });

  it('renders correctly when disabled', () => {
    const component = render(
      <CheckboxList title={question.title} onChange={onChange} values={question.data} disabled />,
    ).toJSON();
    expect(component).toMatchSnapshot();
  });

  it('calls onChange correctly with one option', () => {
    const { getByTestId } = render(
      <CheckboxList title={question.title} onChange={onChange} values={question.data} />,
    );
    fireEvent.press(getByTestId('checkBoxYes'));
    fireEvent.press(getByTestId('checkBox42'));
    expect(onChange).toHaveBeenCalledTimes(2);
  });

  it('calls onChange correctly with 2 options', () => {
    const { getByTestId } = render(
      <CheckboxList
        title={question.title}
        onChange={onChange}
        values={question.data}
        clickableOptions={2}
      />,
    );
    fireEvent.press(getByTestId('checkBoxYes'));
    fireEvent.press(getByTestId('checkBox42'));
    fireEvent.press(getByTestId('checkBoxCats'));
    expect(onChange).toHaveBeenCalledTimes(2);
  });
});
