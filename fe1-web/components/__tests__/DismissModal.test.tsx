import React from 'react';
import { render } from '@testing-library/react-native';
import DismissModal from '../DismissModal';

let setModalIsVisible: Function;

beforeEach(() => {
  setModalIsVisible = jest.fn();
});

describe('DismissModal', () => {
  it('renders correctly', () => {
    const component = render(
      <DismissModal
        visibility
        setVisibility={setModalIsVisible}
        title="Title"
        description="Description"
      />,
    ).toJSON();
    expect(component).toMatchSnapshot();
  });
});
