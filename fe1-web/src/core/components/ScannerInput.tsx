import PropTypes from 'prop-types';
import React from 'react';
import { StyleSheet, View } from 'react-native';

import { Color, Spacing } from 'core/styles';

import Input from './Input';
import PoPButton from './PoPButton';
import PoPIcon from './PoPIcon';

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  button: {
    marginLeft: Spacing.x1,
  },
});

const ScannerInput = (props: IPropTypes) => {
  const { value, placeholder, onChange, onPress, enabled, testID } = props;

  return (
    <View style={styles.container}>
      <Input
        value={value}
        placeholder={placeholder}
        onChange={onChange}
        enabled={enabled}
        testID={testID}
      />
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
  onChange: PropTypes.func,
  onPress: PropTypes.func.isRequired,
  testID: PropTypes.string,
};
ScannerInput.propTypes = propTypes;
ScannerInput.defaultProps = {
  placeholder: '',
  enabled: true,
  onChange: undefined,
  testID: undefined,
};

type IPropTypes = Omit<PropTypes.InferProps<typeof propTypes>, 'onChange'> & {
  onChange: (value: string) => void;
  onPress: () => void;
};

export default ScannerInput;
