import React from 'react';
import PropTypes from 'prop-types';
import DatePickerElement from 'react-datepicker';
import {Timestamp} from "../model/objects";

const DatePicker = (props: IPropTypes) => {
  const { selected } = props;
  const { onChange } = props;

  return (
    <DatePickerElement
      selected={selected}
      onChange={(date: any) => onChange(date)}
      dateFormat="MM/dd/yyyy HH:mm"
      showTimeInput
      minDate={new Date()}
      showDisabledMonthNavigation
    />
  );
};

const propTypes = {
  // eslint-disable-next-line react/forbid-prop-types
  selected: PropTypes.any,
  onChange: PropTypes.func.isRequired,
};
DatePicker.propTypes = propTypes;

DatePicker.defaultProps = {
  selected: undefined,
};

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default DatePicker;

export function dateToTimestamp(date: Date): Timestamp {
  return new Timestamp(Math.floor(date.getTime() / 1000));
}
