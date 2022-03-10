import React, { useMemo } from 'react';
import { StyleSheet, View, ViewStyle } from "react-native";

import { DropdownSelector } from 'core/components';
import PropTypes from 'prop-types';
import { RollCallToken } from '../objects/RollCallToken';

const styles = StyleSheet.create({
  container: {
    maxHeight: 'fit-content',
    maxWidth: 'fit-content',
    marginLeft: 'auto',
    marginRight: 'auto',
  } as ViewStyle,
});
const RollCallTokensDropDown = (props: IPropTypes) => {
  const { onTokenChange } = props;
  const { rollCallTokens } = props;
  const onChange = (pk: string) => {
    onTokenChange(rollCallTokens.find((rct) => rct?.token.publicKey.valueOf() === pk));
  };
  const rctPublicKeys = useMemo(
    () => rollCallTokens.map((rct) => rct?.token.publicKey.valueOf()),
    [rollCallTokens],
  );
  return (
    <View style={styles.container}>
      <DropdownSelector onChange={onChange} values={rctPublicKeys} selected={rctPublicKeys[0]} />
    </View>
  );
};

const propTypes = {
  onTokenChange: PropTypes.func.isRequired,
  rollCallTokens: PropTypes.arrayOf(PropTypes.instanceOf(RollCallToken)).isRequired,
};
RollCallTokensDropDown.propTypes = propTypes;
type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default RollCallTokensDropDown;
