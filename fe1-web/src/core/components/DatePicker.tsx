import PropTypes from 'prop-types';
import React, { forwardRef } from 'react';
import WebDatePicker from 'react-datepicker';

import Input from 'core/components/Input';
import PoPTouchableOpacity from 'core/components/PoPTouchableOpacity';

import 'react-datepicker/dist/react-datepicker.css';

import { datePickerPropTypes, DatePickerPropTypes } from './DatePickerProps';

const CustomInput = forwardRef((props: CustomInputIPropTypes, ref: any) => {
  const { value, onClick } = props;

  return (
    <PoPTouchableOpacity onPress={onClick} ref={ref}>
      <Input value={value || ''} enabled />
    </PoPTouchableOpacity>
  );
});

const customInputPropTypes = {
  value: PropTypes.string,
  onClick: PropTypes.func,
};
CustomInput.propTypes = customInputPropTypes;

CustomInput.defaultProps = {
  value: '',
  onClick: undefined,
};

type CustomInputIPropTypes = PropTypes.InferProps<typeof customInputPropTypes>;

const DatePicker = ({ value, onChange }: DatePickerPropTypes) => {
  return (
    <WebDatePicker
      selected={value}
      onChange={(date: Date) => onChange(date)}
      dateFormat="MM/dd/yyyy HH:mm"
      showTimeInput
      minDate={new Date()}
      showDisabledMonthNavigation
      customInput={<CustomInput />}
    />
  );
};

DatePicker.propTypes = datePickerPropTypes;

export default DatePicker;
