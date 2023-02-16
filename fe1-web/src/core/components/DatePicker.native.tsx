import React from 'react';
import MobileDatePicker from 'react-native-datepicker';

import STRINGS from 'resources/strings';

import { datePickerPropTypes, DatePickerPropTypes } from './DatePickerProps';

const DatePicker = ({ value, onChange }: DatePickerPropTypes) => {
  return (
    <MobileDatePicker
      mode="datetime"
      date={value}
      minDate={new Date()}
      onDateChange={(dateStr, date) => onChange(date)}
      is24Hour
      format="MM/dd/yyyy HH:mm"
      confirmBtnText={STRINGS.general_button_confirm}
      cancelBtnText={STRINGS.general_button_cancel}
    />
  );
};

DatePicker.propTypes = datePickerPropTypes;

export default DatePicker;
