import React from 'react';
import { fireEvent, render } from '@testing-library/react-native';
import STRINGS from 'res/strings';
import TextInputChirp from '../TextInputChirp';

const onChangeText = jest.fn();
const onPress = jest.fn();
const helloWorld = 'Hello World !';

describe('TextInputChirp', () => {
  it('renders correctly without placeholder', () => {
    const { toJSON } = render(
      <TextInputChirp onChangeText={onChangeText} onPress={onPress} />,
    );
    expect(toJSON()).toMatchSnapshot();
  });

  it('renders correctly with placeholder', () => {
    const placeholder = 'Placeholder';
    const { toJSON } = render(
      <TextInputChirp onChangeText={onChangeText} onPress={onPress} placeholder={placeholder} />,
    );
    expect(toJSON()).toMatchSnapshot();
  });

  it('renders correctly when writing less than 300 chars', () => {
    const { getByPlaceholderText, toJSON } = render(
      <TextInputChirp onChangeText={onChangeText} onPress={onPress} />,
    );
    const input = getByPlaceholderText(STRINGS.your_chirp);
    fireEvent.changeText(input, helloWorld);
    expect(toJSON()).toMatchSnapshot();
  });

  it('renders correctly when writing more than 300 chars', () => {
    const bigMessage = 'It seems that this message won\'t fit. It seems that this message won\'t fit. '
      + 'It seems that this message won\'t fit. It seems that this message won\'t fit. It seems that '
      + 'this message won\'t fit. It seems that this message won\'t fit. It seems that this message '
      + 'won\'t fit. It seems that this message won\'t fit.';
    const { getByPlaceholderText, toJSON } = render(
      <TextInputChirp onChangeText={onChangeText} onPress={onPress} />,
    );
    const input = getByPlaceholderText(STRINGS.your_chirp);
    fireEvent.changeText(input, bigMessage);
    expect(toJSON()).toMatchSnapshot();
  });

  it('calls onChangeText correctly', () => {
    const { getByPlaceholderText } = render(
      <TextInputChirp onChangeText={onChangeText} onPress={onPress} />,
    );
    const input = getByPlaceholderText(STRINGS.your_chirp);
    fireEvent.changeText(input, helloWorld);
    expect(onChangeText).toHaveBeenLastCalledWith(helloWorld);
  });

  it('calls onPress correctly', () => {
    const { getByText, getByPlaceholderText } = render(
      <TextInputChirp onChangeText={onChangeText} onPress={onPress} />,
    );
    const input = getByPlaceholderText(STRINGS.your_chirp);
    const publishButton = getByText(STRINGS.button_publish);
    fireEvent.changeText(input, helloWorld);
    fireEvent.press(publishButton);
    expect(onPress).toHaveBeenCalledTimes(1);
  });
});
