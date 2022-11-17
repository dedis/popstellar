import PropTypes from 'prop-types';
import React from 'react';
import { Text } from 'react-native';

const locales: Intl.LocalesArgument | undefined = undefined;
// should produce something along the lines of 'Monday, 1st of November'
const dateOptionsNoYear: Intl.DateTimeFormatOptions = {
  month: 'long',
  weekday: 'long',
  day: 'numeric',
};
// should produce something along the lines of '1st of November 2022'
const dateOptionsWithYear: Intl.DateTimeFormatOptions = {
  year: 'numeric',
  month: 'long',
  day: 'numeric',
};
// HH:MM
const timeOptions: Intl.DateTimeFormatOptions = {
  timeStyle: 'short',
};

const DateRange = ({ start, end }: IPropTypes) => {
  const now = new Date();

  const isSameYear =
    now.getFullYear() === start.getFullYear() && start.getFullYear() === end.getFullYear();

  const isSameMonth = isSameYear && start.getMonth() === end.getMonth();
  const isSameDay = isSameMonth && start.getDate() === end.getDate();

  if (isSameDay) {
    return (
      <Text>
        <Text>{start.toLocaleDateString(locales, dateOptionsNoYear)}</Text>
        {'\n'}
        <Text>{`${start.toLocaleTimeString(locales, timeOptions)} - ${end.toLocaleTimeString(
          locales,
          timeOptions,
        )}`}</Text>
      </Text>
    );
  }

  if (isSameYear) {
    return (
      <Text>
        <Text>{start.toLocaleDateString(locales, dateOptionsNoYear)}</Text>{' '}
        <Text>{start.toLocaleTimeString(locales, timeOptions)}</Text> -{'\n'}
        <Text>{end.toLocaleDateString(locales, dateOptionsNoYear)}</Text>{' '}
        <Text>{end.toLocaleTimeString(locales, timeOptions)}</Text>
      </Text>
    );
  }

  return (
    <Text>
      <Text>{start.toLocaleDateString(locales, dateOptionsWithYear)}</Text>{' '}
      <Text>{start.toLocaleTimeString(locales, timeOptions)}</Text> -{'\n'}
      <Text>{end.toLocaleDateString(locales, dateOptionsWithYear)}</Text>{' '}
      <Text>{end.toLocaleTimeString(locales, timeOptions)}</Text>
    </Text>
  );
};

const propTypes = {
  start: PropTypes.instanceOf(Date).isRequired,
  end: PropTypes.instanceOf(Date).isRequired,
};
DateRange.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default DateRange;
