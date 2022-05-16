import PropTypes from 'prop-types';

export const iconPropTypes = {
  focused: PropTypes.bool,
  color: PropTypes.string.isRequired,
  size: PropTypes.number.isRequired,
};

export const iconDefaultProps = {
  focused: false,
};

export type IconPropTypes = PropTypes.InferProps<typeof iconPropTypes>;
