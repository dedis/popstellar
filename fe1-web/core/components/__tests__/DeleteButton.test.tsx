import React from 'react';
import { render } from '@testing-library/react-native';
import DeleteButton from 'core/components/DeleteButton';

let action: Function;

beforeEach(() => {
  action = jest.fn();
});

describe('DeleteButton', () => {
  it('renders correctly', () => {
    const { toJSON } = render(
      <DeleteButton action={() => action} />,
    );
    expect(toJSON()).toMatchSnapshot();
  });
});
