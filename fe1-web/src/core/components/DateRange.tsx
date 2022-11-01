import PropTypes from 'prop-types';
import React from 'react';
import { Text } from 'react-native';

const DateRange = ({ start, end }: IPropTypes) => {
  const now = new Date();

  const isSameYear =
    now.getFullYear() === start.getFullYear() && start.getFullYear() === end.getFullYear();

  const isSameMonth = isSameYear && start.getMonth() === end.getMonth();
  const isSameDay = isSameMonth && start.getDate() === end.getDate();

  const locales: Intl.LocalesArgument | undefined = undefined;
  const dateOptionsNoYear: Intl.DateTimeFormatOptions = {
    month: 'long',
    weekday: 'long',
    day: 'numeric',
  };
  const dateOptionsWithYear: Intl.DateTimeFormatOptions = {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  };
  const timeOptions: Intl.DateTimeFormatOptions = {
    timeStyle: 'short',
  };

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
