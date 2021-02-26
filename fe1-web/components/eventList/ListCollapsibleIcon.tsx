import React from 'react';
import { Text, TextStyle } from 'react-native';
import PropTypes from 'prop-types';
import { Typography } from 'styles/index';

const ListCollapsibleIcon = (props: IPropTypes) => {
  const { isVisible } = props;
  const { isOpen } = props;

  return (isVisible)
    ? <Text style={[{ ...Typography.base } as TextStyle, { textAlign: 'right' }]}>{isOpen ? 'v' : '<'}</Text>
    : null;
};

const propTypes = {
  isVisible: PropTypes.bool,
  isOpen: PropTypes.bool,
};
ListCollapsibleIcon.propTypes = propTypes;

ListCollapsibleIcon.defaultProps = {
  isVisible: true,
  isOpen: false,
};

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default ListCollapsibleIcon;
