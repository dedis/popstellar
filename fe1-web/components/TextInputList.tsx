import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { View } from 'react-native';
import RemovableTextInput from './RemovableTextInput';

/**
 * Component which creates a list of Text input fields which can be deleted individually
 * Returns array with non-empty Values of the text fields
 * It uses the RemovableTextInput.tsx component to show the individual inputs
 * Input: A function that stores the values that were inputted by the user
 * Output: Array of the unique, nonempty values inputted by the user
 */

const TextInputList = (props: IPropTypes) => {
  const { onChange } = props;
  const [idCount, setIdCount] = useState(1);
  const [userOptions, setUserOptions] = useState([{ id: 0, value: '' }]);

  const updateParent = (options: { id: number, value: string }[]) => {
    // Gets the distinct options which are not empty ('')
    // Set() keeps the order the same
    const distinctValues = [...new Set(options.map((option) => option.value))].filter((value) => value !== '');
    // Updates the values in the parent component
    onChange(distinctValues);
  };

  const addOption = () => {
    // Makes sure each component has a unique ID
    const newOption = { id: idCount, value: '' };
    const newOptions = [...userOptions, newOption];
    setUserOptions(newOptions);
    setIdCount(idCount + 1);
  };

  const updateOption = (id: number, value: string) => {
    const optionIndex = userOptions.findIndex((option) => option.id === id);
    userOptions[optionIndex] = { id: id, value: value };
    setUserOptions(userOptions);
    updateParent(userOptions);
    // If the currently modified text field is the last in the list
    // then it adds a new text input field below
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
