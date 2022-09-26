import { CheckBox } from '@rneui/themed';
import PropTypes from 'prop-types';
import React, { useState } from 'react';
import { StyleSheet, Text, TextStyle, View, ViewStyle } from 'react-native';

import { Typography, ViewStyles } from '../styles';

/**
 * Component with a title and a list of checkboxes below it.
 * Required Input: title (string), values of the checkboxes (string[]) and a function which accepts
 * the selected options
 * Optional Input: clickable options (number), it determines how many options are selectable by
 * the user. Default is 1
 * Returns: index of the selected options
 */

const styles = StyleSheet.create({
  text: {
    ...Typography.baseCentered,
  } as TextStyle,
  view: {
    ...ViewStyles.base,
    flexDirection: 'column',
    zIndex: 3,
  } as ViewStyle,
  flexRow: {
    flexDirection: 'row',
  } as ViewStyle,
});

const CheckboxList = (props: IPropTypes) => {
  const { values } = props;
  const { onChange } = props;
  const { clickableOptions } = props;
  const { title } = props;
  const { disabled } = props;
  const [checked, setChecked] = useState(new Array<boolean>(values.length).fill(false));

  /**
   * Handles a CheckBox press if there are multiple options.
   * A user can always uncheck. He can also check a box if he hasn't reached the max number of
   * selectable options.
   *
   * @param idx - The index of the pressed CheckBox.
   */
  const handleMultipleOptionsPress = (idx: number) => {
    setChecked((prev) => prev.map((item, id) => (idx === id ? !item : item)));
    onChange(
      values
        .map((val, id) => {
          if ((checked[id] && id !== idx) || (id === idx && !checked[id])) {
            return idx;
          }
          return -1;
        })
        .filter((prev) => prev !== -1),
    );
  };

  /**
   * Handles a CheckBox press if there is one option.
   * In this case, the buttons should behave like radio buttons: the user doesn't have to deselect
   * his option to select a new one.
   *
   * @param idx - The index of the pressed CheckBox.
   */
  const handleOneOptionPress = (idx: number) => {
    setChecked((prev) => prev.map((item, id) => idx === id));
    onChange(values.map((val, id) => (id === idx ? id : -1)).filter((prev) => prev !== -1));
  };

  /**
   * Determines whether the user can check or uncheck a given checkbox
   *
   * @param idx - The index of the pressed CheckBox.
   */
  const onCheckBoxPress = (idx: number): void => {
    if (checked[idx] || clickableOptions !== checked.filter(Boolean).length) {
      handleMultipleOptionsPress(idx);
    } else if (clickableOptions === 1) {
      handleOneOptionPress(idx);
    }
  };

  return (
    <View style={styles.view}>
      <Text style={styles.text}>{title}</Text>
      <View style={[styles.view, styles.flexRow]}>
        {values.map((value, idx) => (
          <CheckBox
            key={value}
            disabled={disabled}
            title={value}
            checked={checked[idx]}
            checkedIcon="dot-circle-o"
            uncheckedIcon="circle-o"
            onPress={() => onCheckBoxPress(idx)}
            testID={`checkBox${value}`}
          />
        ))}
      </View>
    </View>
  );
};

const propTypes = {
  values: PropTypes.arrayOf(PropTypes.string).isRequired,
  onChange: PropTypes.func.isRequired,
  clickableOptions: PropTypes.number,
  title: PropTypes.string.isRequired,
  disabled: PropTypes.bool,
};

CheckboxList.propTypes = propTypes;

CheckboxList.defaultProps = {
  clickableOptions: 1,
  disabled: false,
};

type IPropTypes = {
  clickableOptions: number;
  values: string[];
  onChange: Function;
  title: string;
  disabled: boolean;
};

export default CheckboxList;
