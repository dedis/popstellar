import PropTypes from 'prop-types';
import { StyleSheet, TextInput, TextStyle } from 'react-native';
import { Spacing, Typography } from '../styles';

/**
 * Component which creates the typical one line text input used in the application
 */
const styles = StyleSheet.create({
  textInput: {
    ...Typography.base,
    borderBottomWidth: 2,
    marginVertical: Spacing.s,
    marginHorizontal: Spacing.xl,
  } as TextStyle,
});

const selectText = (e: any) => {
  e.target.select();
};

const TextInputLine = (props: IPropTypes) => {
  const { onChangeText } = props;
  let { placeholder } = props;
  let { defaultValue } = props;

  if (placeholder == null) placeholder = undefined;
  if (defaultValue == null) defaultValue = undefined;

  return (
    <TextInput
      style={styles.textInput}
      onChangeText={onChangeText}
      placeholder={placeholder}
      defaultValue={defaultValue}
      onClick={selectText}
    />
  );
};

const propTypes = {
  onChangeText: PropTypes.func.isRequired,
  placeholder: PropTypes.string,
  defaultValue: PropTypes.string,
};
TextInputLine.propTypes = propTypes;

TextInputLine.defaultProps = {
  placeholder: '',
  defaultValue: '',
};

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default TextInputLine;
