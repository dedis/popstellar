import { Picker } from '@react-native-picker/picker';
import PropTypes from 'prop-types';
import React from 'react';

import containerStyles from '../styles/stylesheets/containerStyles';

/**
 * Simple dropdown where the user can select options
 * Inputs:
 *  - String array of all options (required)
 *  - Callback function for when an item is selected (required)
 *      (e.g. {(method: string) => setSelectedElectionMethod(method)} )
 *  - Default selected option (not required, if not specified then first in the array is chosen)
 */

const DropdownSelector = (
  props: Omit<IPropTypes, 'onChange'> & {
    // make the type for 'onChange' more concrete than the inferred type
    onChange: (itemValue: string | null, itemIndex: number) => void;
  },
) => {
  const { selected, onChange, options } = props;

  return (
    <Picker
      selectedValue={selected}
      onValueChange={onChange}
      style={containerStyles.centerWithMargin}>
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

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default DropdownSelector;
