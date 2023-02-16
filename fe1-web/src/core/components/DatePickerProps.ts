import PropTypes from 'prop-types';

import { ExtendType } from 'core/types';

export const datePickerPropTypes = {
  value: PropTypes.instanceOf(Date).isRequired,
  onChange: PropTypes.func.isRequired,
};

export type DatePickerPropTypes = ExtendType<
  PropTypes.InferProps<typeof datePickerPropTypes>,
  {
    onChange: (date: Date) => void;
  }
>;
