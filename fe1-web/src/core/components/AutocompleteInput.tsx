import PropTypes from 'prop-types';
import React, { useCallback } from 'react';
import { View, Text, StyleSheet, ListRenderItemInfo, TextStyle } from 'react-native';
import { FlatList } from 'react-native-gesture-handler';

import { Border, Color, Spacing, Typography } from 'core/styles';

import Input from './Input';
import PoPTouchableOpacity from './PoPTouchableOpacity';

const styles = StyleSheet.create({
  inputContainer: {
    borderWidth: 0,
  },
  suggestionsContainer: {
    position: 'absolute',
    zIndex: 100,
    top: '100%',
    left: 0,
    right: 0,
    maxHeight: 200,
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

const AutocompleteInput = ({
  value,
  suggestions,
  showResults,
  onChange,
  onBlur,
  onFocus,
  placeholder,
  enabled,
  testID,
  customFont,
}: IPropTypes) => {
  const lastSuggestionIndex = suggestions ? suggestions.length - 1 : 0;

  const renderSuggestion = useCallback(
    ({ item: suggestion, index }: ListRenderItemInfo<string>) => (
      <PoPTouchableOpacity
        onPress={() => onChange(suggestion)}
        containerStyle={
          index === lastSuggestionIndex
            ? [styles.suggestionContainer, styles.suggestionContainerLast]
            : styles.suggestionContainer
        }>
        <Text style={[Typography.base, { fontFamily: customFont } as TextStyle]} numberOfLines={1}>
          {suggestion}
        </Text>
      </PoPTouchableOpacity>
    ),
    [lastSuggestionIndex, customFont, onChange],
  );

  return (
    <View>
      <View style={styles.inputContainer}>
        <Input
          value={value}
          placeholder={placeholder}
          onChange={onChange}
          onFocus={onFocus}
          onBlur={onBlur}
          enabled={enabled}
          testID={testID}
          customFont={customFont}
        />
      </View>
      {showResults && (
        <View style={styles.suggestionsContainer}>
          <FlatList
            data={suggestions}
            renderItem={renderSuggestion}
            keyExtractor={(_, index) => index.toString()}
          />
        </View>
      )}
    </View>
  );
};

const propTypes = {
  value: PropTypes.string.isRequired,
  suggestions: PropTypes.arrayOf(PropTypes.string.isRequired).isRequired,
  onChange: PropTypes.func.isRequired,
  placeholder: PropTypes.string,
  enabled: PropTypes.bool,
  showResults: PropTypes.bool,
  onFocus: PropTypes.func,
  onBlur: PropTypes.func,
  testID: PropTypes.string,
  customFont: PropTypes.string,
};

AutocompleteInput.propTypes = propTypes;
AutocompleteInput.defaultProps = {
  placeholder: '',
  enabled: true,
  showResults: false,
  onFocus: undefined,
  onBlur: undefined,
  testID: undefined,
  customFont: '',
};

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default AutocompleteInput;
