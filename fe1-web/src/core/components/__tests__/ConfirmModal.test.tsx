import { fireEvent, render } from '@testing-library/react-native';
import React from 'react';

import ConfirmModal from '../ConfirmModal';

const TITLE = 'Title';
const DESCRIPTION = 'Description';
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

  it('renders correctly with text input', () => {
    const component = render(
      <ConfirmModal
        visibility
        setVisibility={setModalIsVisible}
        title={TITLE}
        description={DESCRIPTION}
        onConfirmPress={() => onConfirmPress()}
        hasTextInput
      />,
    ).toJSON();
    expect(component).toMatchSnapshot();
  });

  it('disappears correctly after dismissing', () => {
    const { getByTestId, toJSON } = render(
      <ConfirmModal
        visibility
        setVisibility={setModalIsVisible}
        title={TITLE}
        description={DESCRIPTION}
        onConfirmPress={() => onConfirmPress()}
      />,
    );
    const cancelButton = getByTestId('modal-header-close');
    fireEvent.press(cancelButton);
    expect(toJSON()).toMatchSnapshot();
  });

  it('calls onConfirmPress correctly', () => {
    const { getByTestId } = render(
      <ConfirmModal
        visibility
        setVisibility={setModalIsVisible}
        title={TITLE}
        description={DESCRIPTION}
        onConfirmPress={() => onConfirmPress()}
        buttonConfirmText={CONFIRM}
      />,
    );
    const confirmButton = getByTestId('confirm-modal-confirm');
    fireEvent.press(confirmButton);
    expect(onConfirmPress).toHaveBeenCalledTimes(1);
  });

  it('calls onConfirmPress with textInput', () => {
    const mockInput = 'input';

    const { getByTestId } = render(
      <ConfirmModal
        visibility
        setVisibility={setModalIsVisible}
        title={TITLE}
        description={DESCRIPTION}
        onConfirmPress={onConfirmPress}
        buttonConfirmText={CONFIRM}
        hasTextInput
      />,
    );
    const textInput = getByTestId('confirm-modal-input');
    fireEvent.changeText(textInput, mockInput);

    const confirmButton = getByTestId('confirm-modal-confirm');
    fireEvent.press(confirmButton);
    expect(onConfirmPress).toHaveBeenCalledWith(mockInput);
  });
});
