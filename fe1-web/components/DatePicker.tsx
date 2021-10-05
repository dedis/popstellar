import React from 'react';
import PropTypes from 'prop-types';
import DatePickerElement from 'react-datepicker';
import { Timestamp } from '../model/objects';

const ONE_MINUTE_IN_SECONDS = 60;

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

/**
 * Transforms a date into a Timestamp.
 *
 * @param date the date to transform
 */
export function dateToTimestamp(date: Date): Timestamp {
  return new Timestamp(Math.floor(date.getTime() / 1000));
}

/**
 * Function called when the user changes the start time. If the date is valid (not in the past),
 * the start time will be set accordingly. Otherwise, the start date will be replaced by the
 * actual time. In both cases, end time is automatically set to start time + 1 hour.
 *
 * @param newStartDate the date the user wants the event to start
 * @param setStartDate function to set the start date of the event
 * @param setEndDate function to set the end date of the event
 */
export function onChangeStartTime(
  newStartDate: Date,
  setStartDate: (value: (((prevState: Timestamp) => Timestamp) | Timestamp)) => void,
  setEndDate: (value: (((prevState: Timestamp) => Timestamp) | Timestamp)) => void,
) {
  const dateStamp: Timestamp = dateToTimestamp(newStartDate);
  const now = new Date();

  if (dateStamp > dateToTimestamp(now)) {
    setStartDate(dateStamp);
    const newEndDate = new Date(newStartDate.getTime());
    newEndDate.setHours(newStartDate.getHours() + 1);
    setEndDate(dateToTimestamp(newEndDate));
  } else {
    setStartDate(dateToTimestamp(now));
    const newEndDate = new Date(now.getTime());
    newEndDate.setHours(now.getHours() + 1);
    setEndDate(dateToTimestamp(newEndDate));
  }
}

/**
 * Function called when the user changes the end time. If the end time is before the start time,
 * it will be set to start time + 60 seconds. Otherwise, the end time of the user will be kept.
 *
 * @param newEndDate the date the user wants the event to end
 * @param startTime the actual start time
 * @param setEndDate function to set the end date of the event
 */
export function onChangeEndTime(
  newEndDate: Date,
  startTime: Timestamp,
  setEndDate: (value: (((prevState: Timestamp) => Timestamp) | Timestamp)) => void,
) {
  const dateTimeStamp: Timestamp = dateToTimestamp(newEndDate);
  if (dateTimeStamp < startTime) {
    setEndDate(startTime.addSeconds(ONE_MINUTE_IN_SECONDS));
  } else {
    setEndDate(dateTimeStamp);
  }
}
