import React from 'react';
import PropTypes from 'prop-types';
import { Picker } from '@react-native-picker/picker';
import { View } from 'react-native';
import styles from 'styles/stylesheets/container';

/**
 * Simple dropdown where the user can select options
 * Inputs:
 *  - String array of all options (required)
 *  - function that stores the selected option (required)
 *      (e.g. {(method: string) => setSelectedElectionMethod(method)} )
 *  - Option that is default selected (not required, default is the first in the array)
 */

const DropdownSelector = (props: IPropTypes) => {
  const { selected } = props;
  const { onChange } = props;
  const { values } = props;
  const options: any = [];
  values.forEach((value) => options.push(<Picker.Item key={value} label={value!} />));

  return (
    <View>
      <Picker
        selectedValue={selected}
        onValueChange={(val: any) => onChange(val)}
        style={styles.centerWithMargin}
      >
        { options }
      </Picker>
    </View>
  );
};

const propTypes = {
  // eslint-disable-next-line react/forbid-prop-types
  selected: PropTypes.any,
  onChange: PropTypes.func.isRequired,
  values: PropTypes.arrayOf(PropTypes.string).isRequired,
};
DropdownSelector.propTypes = propTypes;

DropdownSelector.defaultProps = {
  selected: undefined,
};

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default DropdownSelector;
