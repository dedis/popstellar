import PropTypes from 'prop-types';

import { Color, Icon } from 'core/styles';

export const iconPropTypes = {
  focused: PropTypes.bool,
  color: PropTypes.string.isRequired,
  size: PropTypes.number.isRequired,
};

export const iconDefaultProps = {
  focused: false,
  color: Color.primary,
  size: Icon.size,
};

export type IconPropTypes = PropTypes.InferProps<typeof iconPropTypes>;
