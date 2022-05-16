import PropTypes from 'prop-types';

export const iconPropTypes = {
  focused: PropTypes.bool.isRequired,
  color: PropTypes.string.isRequired,
  size: PropTypes.number.isRequired,
};

export type IconPropTypes = PropTypes.InferProps<typeof iconPropTypes>;
