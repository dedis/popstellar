import PropTypes from 'prop-types';
import React, { useMemo } from 'react';
import { View } from 'react-native';

import RemovableTextInput from './RemovableTextInput';

/**
 * Component which creates a list of Text input fields which can be deleted individually
 * Returns array with non-empty Values of the text fields
 * It uses the RemovableTextInput.tsx component to show the individual inputs
 * Input: A function that stores the values that were inputted by the user
 * Output: Array of the unique, nonempty values inputted by the user
 */

const TextInputList = ({ values, onChange, placeholder, testID }: IPropTypes) => {
  const displayedValues = useMemo(() => {
    return [...values, ''];
  }, [values]);

  return (
    <View>
      {displayedValues.map((text, idx) => (
        // FIXME: Do not use index in key
        // eslint-disable-next-line react/no-array-index-key
        <View key={idx.toString()}>
          <RemovableTextInput
            onChange={(newVal) => {
              // remove last element of displayedValues which is always ''
              const newValues = displayedValues.slice(0, -1);
              newValues[idx] = newVal;
              onChange(newValues);
            }}
            onRemove={() => onChange(values.filter((_, id) => idx !== id))}
            value={text || ''}
            isRemovable={idx !== displayedValues.length - 1}
            placeholder={placeholder}
            testID={testID ? `${testID}_option_${idx}` : undefined}
          />
        </View>
      ))}
    </View>
  );
};

const propTypes = {
  values: PropTypes.arrayOf(PropTypes.string.isRequired).isRequired,
  onChange: PropTypes.func.isRequired,
  placeholder: PropTypes.string,
  testID: PropTypes.string,
};
TextInputList.propTypes = propTypes;
TextInputList.defaultProps = {
  placeholder: '',
  testID: undefined,
};

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default TextInputList;
