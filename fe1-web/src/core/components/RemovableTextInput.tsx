import PropTypes from 'prop-types';
import React from 'react';
import { StyleSheet, View, ViewStyle } from 'react-native';

import { Color, Icon, Spacing } from '../styles';
import Input from './Input';
import PoPIcon from './PoPIcon';
import PoPTouchableOpacity from './PoPTouchableOpacity';

/**
 * TextInput component which is removable by clicking the trashcan
 * It is used by the TextInputList.tsx component
 */

const styles = StyleSheet.create({
  container: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
  } as ViewStyle,
  icon: {
    marginLeft: Spacing.x1,
    marginBottom: Spacing.x1,
  },
});

const RemovableTextInput = (props: IPropTypes) => {
  const { onRemove, onChange, id, value, placeholder, testID } = props;

  return (
    <View style={styles.container} testID={testID || undefined}>
      <Input
        placeholder={placeholder || ''}
        onChange={(text: string) => onChange(id, text)}
        key={id}
        value={value || ''}
        testID={testID ? `${testID}_input` : undefined}
      />
      <PoPTouchableOpacity
        containerStyle={styles.icon}
        onPress={() => onRemove(id)}
        testID={testID ? `${testID}_remove` : undefined}>
        <PoPIcon name="delete" color={Color.primary} size={Icon.size} />
      </PoPTouchableOpacity>
    </View>
  );
};

const propTypes = {
  onRemove: PropTypes.func.isRequired,
  onChange: PropTypes.func.isRequired,
  id: PropTypes.number.isRequired,
  value: PropTypes.string,
  placeholder: PropTypes.string,
  testID: PropTypes.string,
};

RemovableTextInput.propTypes = propTypes;

RemovableTextInput.defaultProps = {
  value: '',
  placeholder: '',
  testID: undefined,
};

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default RemovableTextInput;
