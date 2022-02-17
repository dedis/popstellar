import React from 'react';
import PropTypes from 'prop-types';
import DatePickerElement from 'react-datepicker';
import { Timestamp } from 'model/objects';

const ONE_MINUTE_IN_SECONDS = 60;

/**
 * Component which displays a date picker that allows the user to enter a date and a time. It is
 * impossible to select a date/time from the past.
 */

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
  selected: PropTypes.instanceOf(Date),
  onChange: PropTypes.func.isRequired,
};
DatePicker.propTypes = propTypes;

DatePicker.defaultProps = {
  selected: undefined,
};

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default DatePicker;

/**
 * Function called when the user changes the start time. If the date is valid (not in the past),
 * the start time will be set accordingly. Otherwise, the start date will be replaced by the
 * actual time. In both cases, end time is automatically set to start time + default duration.
 *
 * @param newStartDate - The date the user wants the event to start
 * @param setStartDate - Function to set the start date of the event
 * @param setEndDate - Function to set the end date of the event
 * @param defaultDurationSeconds - The default duration of the event in seconds
 */
export function onChangeStartTime(
  newStartDate: Date,
  setStartDate: React.Dispatch<React.SetStateAction<Timestamp>>,
  setEndDate: React.Dispatch<React.SetStateAction<Timestamp>>,
  defaultDurationSeconds: number,
) {
  const newStart = Timestamp.dateToTimestamp(newStartDate);
  const now = Timestamp.EpochNow();
  let actualStartDate: Date;

  if (newStart.after(now)) {
    setStartDate(newStart);
    actualStartDate = newStartDate;
  } else {
    setStartDate(now);
    actualStartDate = new Date();
  }

  const newEndDate = new Date(actualStartDate.getTime());
  const newEndTimestamp = Timestamp.dateToTimestamp(newEndDate).addSeconds(defaultDurationSeconds);
  setEndDate(newEndTimestamp);
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
  const newEnd = Timestamp.dateToTimestamp(newEndDate);
  if (newEnd.before(startTime)) {
    setEndDate(startTime.addSeconds(ONE_MINUTE_IN_SECONDS));
  } else {
    setEndDate(newEnd);
  }
}
