import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { View } from 'react-native';
import RemovableTextInput from 'components/removableTextInput';

/**
 * Component which creates a list of Text input fields which can be deleted individually
 * Returns array with non-empty Values of the text fields
 */

const TextInputList = (props: IPropTypes) => {
  const { onChange } = props;
  const [idCount, setIdCount] = useState(0);
  const [userOptions, setUserOptions] = useState([{ id: -1, value: '' }]);

  const updateParent = (options: { id: number, value: string }[]) => {
    // Gets the distinct options which are not empty ('')
    // Set() keeps the order the same
    const distinctValues = [...new Set(options.map((option) => option.value))].filter((value) => value !== '');
    // Updates the values in the election setup
    onChange(distinctValues);
  };

  const addOption = () => {
    setIdCount(idCount + 1);
    const newOption = { id: idCount, value: '' };
    const newOptions = [...userOptions, newOption];
    setUserOptions(newOptions);
  };

  const updateOption = (id: number, value: string) => {
    const optionIndex = userOptions.findIndex((option) => option.id === id);
    userOptions[optionIndex] = { id: id, value: value };
    setUserOptions(userOptions);
    updateParent(userOptions);
    // If the currently modified textfield is the last in the list
    // then it adds a new text input field
    if (userOptions.filter((option) => option.id > id).length === 0) {
      addOption();
    }
  };

  const removeOption = (id: number) => {
    // This makes sure that when the last textInput is empty, it can't be deleted
    if (userOptions.filter((option) => option.id > id).length !== 0) {
      // This removes the option
      const filteredOptions = userOptions.filter((option) => option.id !== id);
      setUserOptions(filteredOptions);
      updateParent(filteredOptions);
    }
  };

  // Add value to the text so that it doesn't get removed on each re-render
  return (
    <View>
      {userOptions.map((option) => (
        <RemovableTextInput
          onChange={(id: number, text: string) => { updateOption(id, text); }}
          onRemove={removeOption}
          id={option.id}
          value={option.value}
        />
      ))}
    </View>
  );
};

const propTypes = {
  onChange: PropTypes.func.isRequired,
};
TextInputList.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default TextInputList;
