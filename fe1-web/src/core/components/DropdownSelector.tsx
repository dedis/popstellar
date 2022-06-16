import { Picker } from '@react-native-picker/picker';
import PropTypes from 'prop-types';
import React from 'react';
import { StyleSheet, TextStyle } from 'react-native';

import { Border, Color, Spacing, Typography } from 'core/styles';
import { ExtendType } from 'core/types';

/**
 * Simple dropdown where the user can select options
 * Inputs:
 *  - String array of all options (required)
 *  - Callback function for when an item is selected (required)
 *      (e.g. {(method: string) => setSelectedElectionMethod(method)} )
 *  - Default selected option (not required, if not specified then first in the array is chosen)
 */

const styles = StyleSheet.create({
  select: {
    ...Typography.base,
    marginBottom: Spacing.x1,
    padding: Spacing.x05,
    backgroundColor: Color.contrast,
    borderWidth: 0,
    borderRadius: Border.inputRadius,
  } as TextStyle,
});

const DropdownSelector = (props: IPropTypes) => {
  const { selected, onChange, options } = props;

  return (
    <Picker selectedValue={selected} onValueChange={onChange} style={styles.select}>
      {options.map((option) => (
        <Picker.Item key={option.value} label={option.label} value={option.value} />
      ))}
    </Picker>
  );
};

const propTypes = {
  selected: PropTypes.string,
  onChange: PropTypes.func.isRequired,
  options: PropTypes.arrayOf(
    PropTypes.shape({
      label: PropTypes.string.isRequired,
      value: PropTypes.string.isRequired,
    }).isRequired,
  ).isRequired,
};
DropdownSelector.propTypes = propTypes;

DropdownSelector.defaultProps = {
  selected: undefined,
};

type IPropTypes = ExtendType<
  PropTypes.InferProps<typeof propTypes>,
  {
    // make the type for 'onChange' more concrete than the inferred type
    onChange: (itemValue: string | null, itemIndex: number) => void;
  }
>;

export default DropdownSelector;
