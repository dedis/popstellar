import PropTypes from 'prop-types';
import React from 'react';
import { StyleSheet, View } from 'react-native';

import { Color, Spacing, Typography } from 'core/styles';

import AutocompleteInput from './AutocompleteInput';
import { inputStyleSheet } from './Input';
import PoPButton from './PoPButton';
import PoPIcon from './PoPIcon';

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    alignItems: 'center',
    zIndex: 99,
  },
  button: {
    marginLeft: Spacing.x1,
  },
  autocompleteContainer: {
    flex: 1,
  },
});

const ScannerInput = ({
  value,
  suggestions,
  placeholder,
  onChange,
  onPress,
  onFocus,
  onBlur,
  enabled,
  testID,
}: IPropTypes) => {
  const inputStyles = [Typography.paragraph, inputStyleSheet.input];

  if (!enabled) {
    inputStyles.push(inputStyleSheet.disabled);
  }

  return (
    <View style={styles.container}>
      <View style={styles.autocompleteContainer}>
        <AutocompleteInput
          suggestions={suggestions || []}
          showResults={
            suggestions &&
            suggestions.length !== 0 &&
            !(suggestions.length === 1 && suggestions[0] === value)
          }
          onFocus={onFocus || undefined}
          onBlur={onBlur || undefined}
          value={value}
          placeholder={placeholder || undefined}
          onChange={onChange}
          testID={testID || undefined}
        />
      </View>
      {/* <Input
        value={value}
        placeholder={placeholder}
        onChange={onChange}
        enabled={enabled}
        testID={testID}
        /> */}
      <View style={styles.button}>
        <PoPButton onPress={onPress}>
          <PoPIcon name="scan" color={Color.contrast} />
        </PoPButton>
      </View>
    </View>
  );
};

const propTypes = {
  enabled: PropTypes.bool,
  placeholder: PropTypes.string,
  value: PropTypes.string.isRequired,
  suggestions: PropTypes.arrayOf(PropTypes.string.isRequired),
  onChange: PropTypes.func,
  onPress: PropTypes.func.isRequired,
  onFocus: PropTypes.func,
  onBlur: PropTypes.func,
  testID: PropTypes.string,
};
ScannerInput.propTypes = propTypes;
ScannerInput.defaultProps = {
  placeholder: '',
  suggestions: [],
  enabled: true,
  onChange: undefined,
  onFocus: undefined,
  onBlur: undefined,
  testID: undefined,
};

type IPropTypes = Omit<PropTypes.InferProps<typeof propTypes>, 'onChange'> & {
  onChange: (value: string) => void;
  onPress: () => void;
};

export default ScannerInput;
