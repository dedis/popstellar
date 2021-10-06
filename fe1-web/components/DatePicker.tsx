import React from 'react';
import PropTypes from 'prop-types';
import DatePickerElement from 'react-datepicker';
import { Timestamp } from '../model/objects';

const ONE_MINUTE_IN_SECONDS = 60;
const ONE_SECOND_IN_MILLIS = 1000;

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
 * @param date - The date to transform
 */
export function dateToTimestamp(date: Date): Timestamp {
  return new Timestamp(Math.floor(date.getTime() / ONE_SECOND_IN_MILLIS));
}

/**
 * Function called when the user changes the start time. If the date is valid (not in the past),
 * the start time will be set accordingly. Otherwise, the start date will be replaced by the
 * actual time. In both cases, end time is automatically set to start time + 1 hour.
 *
 * @param newStartDate - The date the user wants the event to start
 * @param setStartDate - Function to set the start date of the event
 * @param setEndDate - Function to set the end date of the event
 */
export function onChangeStartTime(
  newStartDate: Date,
  setStartDate: React.Dispatch<React.SetStateAction<Timestamp>>,
  setEndDate: React.Dispatch<React.SetStateAction<Timestamp>>,
) {
  const newStartTimestamp: Timestamp = dateToTimestamp(newStartDate);
  const dateNow = new Date();
  const timestampNow = dateToTimestamp(dateNow);
  let actualStartDate: Date;

  if (newStartTimestamp > timestampNow) {
    setStartDate(newStartTimestamp);
    actualStartDate = newStartDate;
  } else {
    setStartDate(timestampNow);
    actualStartDate = dateNow;
  }

  const newEndDate = new Date(actualStartDate.getTime());
  newEndDate.setHours(actualStartDate.getHours() + 1);
  setEndDate(dateToTimestamp(newEndDate));
}

/**
 * Function called when the user changes the end time. If the end time is before the start time,
 * it will be set to start time + 60 seconds. Otherwise, the end time of the user will be kept.
 *
 * @param newEndDate - The date the user wants the event to end
 * @param startTime - The actual start time
 * @param setEndDate - Function to set the end date of the event
 */
export function onChangeEndTime(
  newEndDate: Date,
  startTime: Timestamp,
  setEndDate: React.Dispatch<React.SetStateAction<Timestamp>>,
) {
  const newEndTimestamp: Timestamp = dateToTimestamp(newEndDate);
  if (newEndTimestamp < startTime) {
    setEndDate(startTime.addSeconds(ONE_MINUTE_IN_SECONDS));
  } else {
    setEndDate(newEndTimestamp);
  }
}
