import PropTypes from 'prop-types';
import React from 'react';
import { StyleSheet, TextInput, TextStyle, View, ViewStyle } from 'react-native';

import { Border, Spacing, Typography } from '../styles';
import CopyButton from './CopyButton';

/**
 * This is a TextInput component which data is copiable
 * to clipboard by clicking the copy button.
 */

const styles = StyleSheet.create({
  view: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    marginVertical: Spacing.x1,
  } as ViewStyle,
  textInput: {
    ...Typography.base,
    ...Typography.centered,
    /* will make it shrink down to a width of 50 */
    width: 50,
    flexGrow: 1,
    borderWidth: Border.width,
    borderRadius: Border.radius,
    padding: Spacing.x05,
    marginRight: Spacing.x1,
  } as TextStyle,
});

const CopiableTextInput = (props: IPropTypes) => {
  const { text, negative } = props;

  return (
    <View style={styles.view}>
      <TextInput
        value={text || ''}
        editable={false}
        style={
          negative
            ? [styles.textInput, Typography.negative, Border.negativeColor]
            : styles.textInput
        }
        selectTextOnFocus
      />
      <CopyButton data={text || ''} negative={negative} />
    </View>
  );
};

const propTypes = {
  text: PropTypes.string,
  negative: PropTypes.bool,
};

CopiableTextInput.propTypes = propTypes;

CopiableTextInput.defaultProps = {
  text: '',
  negative: false,
};

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default CopiableTextInput;
