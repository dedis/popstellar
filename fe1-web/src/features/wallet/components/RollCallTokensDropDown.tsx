import { Picker } from '@react-native-picker/picker';
import PropTypes from 'prop-types';
import React from 'react';
import { StyleSheet } from 'react-native';

import { RollCallToken } from '../objects/RollCallToken';

const styles = StyleSheet.create({
  pickerStyle: {
    maxHeight: 40,
    width: 'fit-content',
    maxWidth: '90%',
    fontSize: 20,
    textAlign: 'center',
    margin: 'auto',
  },
});
const RollCallTokensDropDown = (props: IPropTypes) => {
  const { onTokenChange } = props;
  const { rollCallTokens } = props;

  const options = rollCallTokens.map((rc) => {
    const value = rc?.token.publicKey.valueOf() || '';
    return <Picker.Item key={value} label={value} />;
  });

  const onChange = (pk: string) => {
    const found = rollCallTokens.find((rct) => rct?.token.publicKey.valueOf() === pk);
    if (found) {
      onTokenChange(found);
    }
  };

  return (
    <Picker onValueChange={(val: any) => onChange(val)} style={styles.pickerStyle}>
      {options}
    </Picker>
  );
};

const propTypes = {
  onTokenChange: PropTypes.func.isRequired,
  rollCallTokens: PropTypes.arrayOf(PropTypes.instanceOf(RollCallToken)).isRequired,
};
RollCallTokensDropDown.propTypes = propTypes;
type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default RollCallTokensDropDown;
