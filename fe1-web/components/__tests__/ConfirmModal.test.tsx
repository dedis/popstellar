import React from 'react';
import { fireEvent, render } from '@testing-library/react-native';
import ConfirmModal from '../ConfirmModal';

const TITLE = 'Title';
const DESCRIPTION = 'Description';
const CANCEL = 'Cancel';
const CONFIRM = 'Confirm';

let setModalIsVisible: Function;
let onConfirmPress: Function;

beforeEach(() => {
  setModalIsVisible = jest.fn();
  onConfirmPress = jest.fn();
});

describe('ConfirmModal', () => {
  it('renders correctly', () => {
    const component = render(
      <ConfirmModal
        visibility
        setVisibility={setModalIsVisible}
        title={TITLE}
        description={DESCRIPTION}
        onConfirmPress={() => onConfirmPress()}
      />,
    ).toJSON();
    expect(component).toMatchSnapshot();
  });

  it('disappears correctly after dismissing', () => {
    const { getByText, toJSON } = render(
      <ConfirmModal
        visibility
        setVisibility={setModalIsVisible}
        title={TITLE}
        description={DESCRIPTION}
        onConfirmPress={() => onConfirmPress()}
        buttonCancelText={CANCEL}
      />,
    );
    const cancelButton = getByText(CANCEL);
    fireEvent.press(cancelButton);
    expect(toJSON()).toMatchSnapshot();
  });

  it('calls onConfirmPress correctly', () => {
    const { getByText } = render(
      <ConfirmModal
        visibility
        setVisibility={setModalIsVisible}
        title={TITLE}
        description={DESCRIPTION}
        onConfirmPress={() => onConfirmPress()}
        buttonConfirmText={CONFIRM}
      />,
    );
    const confirmButton = getByText(CONFIRM);
    fireEvent.press(confirmButton);
    expect(onConfirmPress).toHaveBeenCalledTimes(1);
  });
});
