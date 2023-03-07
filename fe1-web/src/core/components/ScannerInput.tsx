import PropTypes from 'prop-types';
import React from 'react';
import { StyleSheet, View } from 'react-native';

import { Color, Spacing } from 'core/styles';

import AutocompleteInput from './AutocompleteInput';
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
  isMonospaced,
}: IPropTypes) => {
  return (
    <View style={styles.container}>
      <View style={styles.autocompleteContainer}>
        <AutocompleteInput
          suggestions={suggestions || []}
          enabled={enabled}
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
          isMonospaced={isMonospaced}
        />
      </View>
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
  onChange: PropTypes.func.isRequired,
  onPress: PropTypes.func.isRequired,
  onFocus: PropTypes.func,
  onBlur: PropTypes.func,
  testID: PropTypes.string,
  isMonospaced: PropTypes.bool,
};
ScannerInput.propTypes = propTypes;
ScannerInput.defaultProps = {
  placeholder: '',
  suggestions: [],
  enabled: true,
  onFocus: undefined,
  onBlur: undefined,
  testID: undefined,
  isMonospaced: false,
};

type IPropTypes = Omit<PropTypes.InferProps<typeof propTypes>, 'onChange'> & {
  onChange: (value: string) => void;
  onPress: () => void;
};

export default ScannerInput;
