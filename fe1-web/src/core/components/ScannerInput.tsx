import PropTypes from 'prop-types';
import React from 'react';
import { StyleSheet, Text, View } from 'react-native';
import AutocompleteInput from 'react-native-autocomplete-input';

import { Border, Color, Spacing, Typography } from 'core/styles';

import { inputStyleSheet } from './Input';
import PoPButton from './PoPButton';
import PoPIcon from './PoPIcon';
import PoPTouchableOpacity from './PoPTouchableOpacity';

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
  inputContainer: {
    borderWidth: 0,
  },
  suggestionsContainer: {
    position: 'absolute',
    zIndex: 100,
    top: '100%',
    left: 0,
    right: 0,
    backgroundColor: Color.contrast,
    borderRadius: Border.inputRadius,
    shadowColor: Color.primary,
    shadowOpacity: 0.1,
    shadowRadius: 3,
    shadowOffset: {
      height: 5,
      width: 5,
    },
  },
  suggestionContainer: {
    paddingVertical: Spacing.x05,
    paddingHorizontal: Spacing.x1,
    borderBottomColor: Color.separator,
    borderBottomWidth: 1,
  },
  suggestionContainerLast: {
    borderRadius: Border.inputRadius,
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
          data={suggestions || []}
          hideResults={
            !suggestions ||
            suggestions.length === 0 ||
            (suggestions.length === 1 && suggestions[0] === value)
          }
          onFocus={onFocus || undefined}
          onBlur={onBlur || undefined}
          value={value}
          placeholder={placeholder || undefined}
          placeholderTextColor={Color.inactive}
          onChangeText={onChange}
          flatListProps={{
            keyExtractor: (i: string) => i,
            renderItem: ({ item, index }) => (
              <PoPTouchableOpacity
                onPress={() => onChange(item)}
                containerStyle={
                  index === (suggestions || []).length - 1
                    ? [styles.suggestionContainer, styles.suggestionContainerLast]
                    : styles.suggestionContainer
                }>
                <Text style={Typography.base} numberOfLines={1}>
                  {item}
                </Text>
              </PoPTouchableOpacity>
            ),
          }}
          testID={testID || undefined}
          style={inputStyles}
          inputContainerStyle={[inputStyleSheet.container, styles.inputContainer]}
          listContainerStyle={styles.suggestionsContainer}
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
