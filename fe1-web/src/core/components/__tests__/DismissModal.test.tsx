import React from 'react';
import { fireEvent, render } from '@testing-library/react-native';
import DismissModal from 'core/components/DismissModal';

const TITLE = 'Title';
const DESCRIPTION = 'Description';
const OK = 'Ok';

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
        title={TITLE}
        description={DESCRIPTION}
      />,
    ).toJSON();
    expect(component).toMatchSnapshot();
  });

  it('disappears correctly after dismissing', () => {
    const { getByText, toJSON } = render(
      <DismissModal
        visibility
        setVisibility={setModalIsVisible}
        title={TITLE}
        description={DESCRIPTION}
        buttonText={OK}
      />,
    );
    const button = getByText(OK);
    fireEvent.press(button);
    expect(toJSON()).toMatchSnapshot();
  });
});
