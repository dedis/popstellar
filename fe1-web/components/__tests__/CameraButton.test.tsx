import React from 'react';
import { render } from '@testing-library/react-native';
import CameraButton from '../CameraButton';

let action: Function;

beforeEach(() => {
  action = jest.fn();
});

describe('CameraButton', () => {
  it('renders correctly', () => {
    const { toJSON } = render(
      <CameraButton action={action} />,
    );
    expect(toJSON()).toMatchSnapshot();
  });
});
